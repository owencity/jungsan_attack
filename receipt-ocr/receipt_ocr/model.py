from dataclasses import dataclass, field
import math


@dataclass(frozen=True)
class Token:
    text: str
    confidence: float
    box: tuple[float, float, float, float]

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

    def to_dict(self):
        return {
            "name": self.name, "category": self.category, "unit": self.unit,
            "unitPrice": self.unit_price, "quantity": self.quantity,
            "lineAmount": self.line_amount, "confidence": self.confidence,
            "reviewReasons": self.review_reasons,
        }


@dataclass
class Receipt:
    total: int | None
    drinks: list[Drink]
    warnings: list[str]

    def to_dict(self):
        return {
            "schemaVersion": 1, "total": self.total,
            "drinks": [drink.to_dict() for drink in self.drinks],
            "warnings": self.warnings, "requiresConfirmation": True,
        }
