from dataclasses import dataclass, field
import math


@dataclass(frozen=True)
class Token:
    text: str
    confidence: float
    box: tuple[float, float, float, float]
    recovered_numeric: bool = False
    numeric_column: str | None = None

    def __post_init__(self):
        if not math.isfinite(self.confidence) or not 0 <= self.confidence <= 1:
            raise ValueError("invalid confidence")
        if len(self.box) != 4 or not all(math.isfinite(value) for value in self.box):
            raise ValueError("invalid box")
        if self.box[2] <= self.box[0] or self.box[3] <= self.box[1]:
            raise ValueError("invalid box")


@dataclass
class Drink:
    name: str
    category: str
    unit: str
    unit_price: int | None
    quantity: int | None
    line_amount: int | None
    confidence: float
    review_reasons: list[str] = field(default_factory=list)
    serving_type: str = "UNKNOWN"
    volume_ml: int | None = None

    @property
    def bottle_count(self):
        if self.category == "UNKNOWN":
            return None
        if any(reason in self.review_reasons for reason in
               ("UNIT_CONFLICT", "QUANTITY_INVALID_OR_CONFLICT", "ITEM_OUT_OF_RANGE",
                "ITEM_AMOUNT_MISMATCH", "LOW_CONFIDENCE", "COMBO_OR_PROMOTION", "RETURN_OR_CANCEL")):
            return None
        if self.unit == "BOTTLE":
            return self.quantity
        if self.unit in {"CAN", "GLASS", "PITCHER"} or self.serving_type == "DRAFT":
            return 0
        return None

    def to_dict(self):
        return {
            "name": self.name, "category": self.category, "unit": self.unit,
            "unitPrice": self.unit_price, "quantity": self.quantity,
            "lineAmount": self.line_amount, "confidence": self.confidence,
            "reviewReasons": self.review_reasons,
            "servingType": self.serving_type, "volumeMl": self.volume_ml,
            "bottleCount": self.bottle_count,
        }


@dataclass
class Receipt:
    total: int | None
    drinks: list[Drink]
    warnings: list[str]

    def to_dict(self):
        return {
            "schemaVersion": 2, "total": self.total,
            "drinks": [drink.to_dict() for drink in self.drinks],
            "warnings": self.warnings, "requiresConfirmation": True,
        }
