from io import BytesIO
import warnings
from PIL import Image, ImageOps, UnidentifiedImageError


MAX_BYTES = 8 * 1024 * 1024
MAX_PIXELS = 16_000_000
MAX_EDGE = 2400


class InvalidImage(ValueError):
    pass


def decode_image(payload: bytes) -> Image.Image:
    if not payload or len(payload) > MAX_BYTES:
        raise InvalidImage("IMAGE_SIZE_INVALID")
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(BytesIO(payload)) as original:
                if original.format not in {"JPEG", "PNG"}:
                    raise InvalidImage("IMAGE_FORMAT_UNSUPPORTED")
                if original.width * original.height > MAX_PIXELS:
                    raise InvalidImage("IMAGE_DIMENSIONS_TOO_LARGE")
                if getattr(original, "n_frames", 1) != 1:
                    raise InvalidImage("IMAGE_FORMAT_UNSUPPORTED")
                original.load()
                image = ImageOps.exif_transpose(original).convert("RGB")
                image.thumbnail((MAX_EDGE, MAX_EDGE))
                # 승인번호·카메라 위치 등 메타데이터는 OCR에 필요하지 않다.
                image.info.clear()
                return image
    except InvalidImage:
        raise
    except (UnidentifiedImageError, OSError, ValueError,
            Image.DecompressionBombError, Image.DecompressionBombWarning) as error:
        raise InvalidImage("IMAGE_INVALID") from error
