from math import ceil
from fractions import Fraction as F

def settle(participants, rounds, extras, attendance, exempt=(), unit=10):
    """
    participants: [id]
    rounds:  [{'id','total','alcohol','payer'}]
    extras:  [{'id','label','amount','bearers':[id],'payer'}]
    attendance: {(pid,rid): (attended, drank)}
    exempt: 면제자 id 집합 (부담 0, 모든 분모에서 제외)
    """
    ex = set(exempt)
    raw = {p: F(0) for p in participants}

    for r in rounds:
        att = [p for p in participants if attendance.get((p, r['id']), (False,False))[0] and p not in ex]
        drk = [p for p in participants if attendance.get((p, r['id']), (False,False))[1] and p not in ex]
        alc = F(r['alcohol']); food = F(r['total']) - alc
        if att:
            for p in att: raw[p] += food/len(att)
        if drk:
            for p in drk: raw[p] += alc/len(drk)

    for e in extras:
        bs = [p for p in e['bearers'] if p not in ex]
        if bs:
            for p in bs: raw[p] += F(e['amount'])/len(bs)

    paid = {p:0 for p in participants}
    for r in rounds: paid[r['payer']] += r['total']
    for e in extras: paid[e['payer']] += e['amount']
    grand = sum(r['total'] for r in rounds) + sum(e['amount'] for e in extras)

    # 흡수자: 면제자 제외, 결제총액 최대, 동률이면 id 사전순
    cand = [p for p in participants if p not in ex]
    absorber = sorted(cand, key=lambda p: (-paid[p], p))[0]

    final = {}
    for p in participants:
        if p in ex: final[p] = 0
        elif p == absorber: continue
        else: final[p] = int(ceil(raw[p]/unit)*unit)
    final[absorber] = grand - sum(final.values())

    bal = {p: paid[p]-final[p] for p in participants}
    cr = sorted([[p,v] for p,v in bal.items() if v>0], key=lambda x:(-x[1],x[0]))
    de = sorted([[p,-v] for p,v in bal.items() if v<0], key=lambda x:(-x[1],x[0]))
    tr=[]; ci=di=0
    while ci<len(cr) and di<len(de):
        amt=min(cr[ci][1],de[di][1])
        if amt>0: tr.append((de[di][0],cr[ci][0],amt))
        cr[ci][1]-=amt; de[di][1]-=amt
        if cr[ci][1]==0: ci+=1
        if de[di][1]==0: di+=1
    return absorber, final, tr, grand

def show(name, ps, rs, ex_items, att, exempt=(), unit=10):
    ab, fin, tr, g = settle(ps, rs, ex_items, att, exempt, unit)
    print(f"--- {name}")
    print("  흡수자:", ab, "| 면제:", list(exempt) or "없음")
    for p in ps: print(f"   {p}: {fin[p]:,}원" + ("  (면제)" if p in exempt else ""))
    ok = sum(fin.values())==g
    print(f"   합계 {sum(fin.values()):,} / 원금 {g:,} → {'OK' if ok else 'MISMATCH'}")
    for d,c,a in tr: print(f"   송금 {d} → {c} : {a:,}원")
    print()

P4=["동규","민지","재훈","수아"]
A4={**{(p,1):(True,True) for p in P4}}

# T8 면제
show("T8 면제 — 4명 1차 40,000(술 0), 수아 생일이라 면제, 동규 결제",
     P4, [{'id':1,'total':40000,'alcohol':0,'payer':"동규"}], [],
     {(p,1):(True,False) for p in P4}, exempt=["수아"])

# T9 기타 항목
show("T9 기타항목 — 1차 40,000(전원,술0) 동규결제 + 택시비 18,000(재훈·수아만) 재훈결제",
     P4, [{'id':1,'total':40000,'alcohol':0,'payer':"동규"}],
     [{'id':101,'label':'택시비','amount':18000,'bearers':["재훈","수아"],'payer':"재훈"}],
     {(p,1):(True,False) for p in P4})

# T10 술병 계산 (소주5×5000 + 맥주3×6000 = 43,000)
show("T10 술병 — 1차 총 91,000 중 술값 43,000(소주5×5,000 + 맥주3×6,000), 수아 논알콜, 동규결제",
     P4, [{'id':1,'total':91000,'alcohol':43000,'payer':"동규"}], [],
     {("동규",1):(True,True),("민지",1):(True,True),("재훈",1):(True,True),("수아",1):(True,False)})

# T11 종합
P5=["동규","민지","재훈","수아","지원"]
show("T11 종합 — 1차 87,000(술35,000) 동규결제 / 2차 42,000(술30,000) 민지결제 / 택시 20,000(수아·지원) 민지결제 / 지원 면제",
     P5,
     [{'id':1,'total':87000,'alcohol':35000,'payer':"동규"},
      {'id':2,'total':42000,'alcohol':30000,'payer':"민지"}],
     [{'id':101,'label':'택시비','amount':20000,'bearers':["수아","지원"],'payer':"민지"}],
     {("동규",1):(True,True),("민지",1):(True,True),("재훈",1):(True,True),("수아",1):(True,True),("지원",1):(True,False),
      ("동규",2):(True,True),("민지",2):(True,True),("재훈",2):(True,True)},
     exempt=["지원"])
