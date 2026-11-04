"""새 매장 평가 자료와 별개로 기존 개발 사진에서 확인한 전체 표기만 지원한다."""
from pathlib import Path
import unittest

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from test_extract import tokens


class ObservedAliasesRound2Tests(unittest.TestCase):
    def setUp(self):
        self.dictionary = load_dictionary(Path(__file__).resolve().parents[1] / "data/drinks.md")

    def test_exact_daesun_keeps_unknown_container_and_subtype(self):
        result = extract(tokens("상품명 단가 수량 금액", "대선 4,000 2 8,000"), self.dictionary)
        drink = result.drinks[0]
        self.assertEqual(("SOJU", "대선 (세부형 미상)", 4000, 2, 8000),
                         (drink.category, drink.name, drink.unit_price, drink.quantity, drink.line_amount))
        self.assertEqual(("UNKNOWN", None, None), (drink.unit, drink.volume_ml, drink.bottle_count))
        self.assertTrue(result.to_dict()["requiresConfirmation"])
        # 기존의 명시 잔 단위는 유지하되 브랜드가 병 수를 만들지는 않는다.
        glass = extract(tokens("상품명 단가 수량 금액", "대선 잔 4,000 2 8,000"), self.dictionary).drinks[0]
        self.assertEqual(("GLASS", 2, 0), (glass.unit, glass.quantity, glass.bottle_count))

    def test_unregistered_qualifier_and_non_alcohol_are_not_truncated(self):
        for name in ("대선제로", "대선 무알콜", "대선소주잔", "대선(미등록)", "대선맥주"):
            with self.subTest(name=name):
                self.assertEqual([], extract(tokens("상품명 단가 수량 금액", name + " 4,000 2 8,000"),
                                             self.dictionary).drinks)
