"""운영 Docker/SSH를 호출하지 않고 배포 스크립트의 실패 분기와 이미지 복원을 시험한다."""
import os, pathlib, shutil, subprocess, tempfile, sys

sys.stdout.reconfigure(encoding="utf-8")
repo=pathlib.Path(__file__).resolve().parents[1]
bash=pathlib.Path("C:/Program Files/Git/bin/bash.exe") if os.name=="nt" else pathlib.Path("/bin/bash")
target="jeongsan-server:"+"a"*40
previous="jeongsan-server:"+"b"*40

def shellpath(path):
    text=path.as_posix()
    return "/"+text[0].lower()+text[2:] if os.name=="nt" else text

temporary_parent=(repo/"server"/"build").resolve()
temporary_parent.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory(prefix="jeongsan-deploy-test-",dir=temporary_parent) as folder:
    root=pathlib.Path(folder)
    assert root.resolve().parent==temporary_parent and root.name.startswith("jeongsan-deploy-test-")
    destination=root/"scripts"/"deploy"
    destination.mkdir(parents=True)
    for file in (repo/"scripts"/"deploy").glob("*.sh"):
        (destination/file.name).write_bytes(file.read_bytes())
    (root/".env").write_text("DB_PASSWORD=test-only\n")
    (root/"docker-compose.prod.yml").write_text("services: {}\n")
    fake=root/"bin"
    fake.mkdir()
    mock='''#!/usr/bin/env bash
set -eu
case "$(basename "$0")" in
  flock) [ "$SCENARIO" != busy ]; exit ;;
  curl) printf '{"status":"UP"}'; exit ;;
esac
if [ "$1" = inspect ]; then
  if [[ "$*" == *Config.Image* ]]; then cat "$MOCK_STATE";
  elif [ "$SCENARIO" = rollback ] && [ "$(cat "$MOCK_STATE")" = "$TARGET" ]; then echo unhealthy;
  else echo healthy; fi
elif [ "$1" = compose ]; then
  if [[ "$*" == *' ps -q app'* ]]; then echo mock-container;
  elif [[ "$*" == *' port app 8080'* ]]; then
    if [ "$SCENARIO" = wrongport ]; then echo 127.0.0.1:8080; else echo 127.0.0.1:18080; fi
  elif [[ "$*" == *' up -d'* ]]; then printf '%s' "$APP_IMAGE" > "$MOCK_STATE"; fi
fi
'''
    for name in ("docker","curl","flock"):
        file=fake/name
        file.write_text(mock,newline="\n")
        file.chmod(0o755)
    state=root/"current-image"
    environment=os.environ.copy()
    environment.update(PATH=shellpath(fake)+":"+environment.get("PATH",""),MOCK_STATE=shellpath(state),TARGET=target,TRIES="1",INTERVAL="0")
    # Git Bash는 Windows PATH를 자동 변환하므로 fake 경로는 별도 셸 인자로 앞에 붙인다.
    environment["FAKE_BIN"]=shellpath(fake)
    def run(script,scenario,args=(),expected=0,image=None):
        state.write_text(image or target)
        env=environment.copy()
        env["SCENARIO"]=scenario
        if script=="health.sh": env["APP_IMAGE"]=target
        else: env.pop("APP_IMAGE",None)
        command='export PATH="$FAKE_BIN:$PATH"; bash "$1" "${@:2}"'
        reply=subprocess.run([str(bash),"-c",command,"test",shellpath(destination/script),*args],env=env,capture_output=True,text=True,encoding="utf-8")
        assert (reply.returncode==0)==(expected==0),(scenario,reply.returncode,reply.stdout,reply.stderr)
    run("health.sh","healthy")
    print("PASS: 동일 이미지·healthy·18080 HTTP는 성공")
    run("health.sh","wrongport",expected=1)
    print("PASS: 다른 앱의 8080 HTTP UP은 실패")
    run("health.sh","healthy",expected=1,image=previous)
    print("PASS: 컨테이너 이미지가 다르면 실패")
    run("deploy.sh","healthy",args=("",target))
    assert (root/".release.env").read_text().strip()=="APP_IMAGE="+target
    print("PASS: 검증된 SHA만 성공 이력 저장")
    (root/".release.env").write_text("APP_IMAGE="+previous+"\n")
    run("deploy.sh","rollback",args=("",target),expected=1,image=previous)
    assert state.read_text()==previous and (root/".release.env").read_text().strip()=="APP_IMAGE="+previous
    print("PASS: 새 앱 실패 시 이전 이미지 복원·성공 이력 보존")
    run("deploy.sh","busy",args=("",target),expected=1)
    print("PASS: 서버 배포 잠금 획득 실패 시 실행 중단 (flock 자체 동시성은 운영 도구의 책임)")
print("DEPLOY: 6 checks passed")
