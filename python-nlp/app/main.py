from __future__ import annotations

from dataclasses import dataclass
from typing import List

import jieba
import jieba.analyse
from fastapi import FastAPI
from pydantic import BaseModel, Field
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import Pipeline
from sklearn.svm import LinearSVC


class AnalyzeRequest(BaseModel):
    title: str = Field(min_length=1)
    content: str = Field(min_length=1)


class AnalyzeResponse(BaseModel):
    predicted_category: str
    confidence: float
    keywords: List[str]


@dataclass
class ModelBundle:
    pipeline: Pipeline


TRAIN_TEXTS = [
    "教学设计 教案 课堂 目标 课程内容",
    "实验报告 课程实验 数据分析 结果",
    "毕业论文 参考文献 摘要 研究方法",
    "课程通知 教学安排 课堂管理",
    "论文综述 相关工作 模型评估",
]

TRAIN_LABELS = ["教案", "实验", "论文", "通知", "论文"]


def train_classifier() -> ModelBundle:
    pipeline = Pipeline(
        [
            ("tfidf", TfidfVectorizer(tokenizer=lambda x: jieba.lcut(x), token_pattern=None)),
            ("clf", LinearSVC()),
        ]
    )
    pipeline.fit(TRAIN_TEXTS, TRAIN_LABELS)
    return ModelBundle(pipeline=pipeline)


app = FastAPI(title="Course Document NLP Service", version="1.0.0")
model_bundle = train_classifier()


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/classify")
def classify(req: AnalyzeRequest) -> dict:
    text = f"{req.title} {req.content}"
    label = model_bundle.pipeline.predict([text])[0]
    score = float(model_bundle.pipeline.decision_function([text]).max())
    confidence = 1 / (1 + pow(2.71828, -score))
    return {"predicted_category": str(label), "confidence": round(confidence, 4)}


@app.post("/keywords")
def keywords(req: AnalyzeRequest, top_k: int = 8) -> dict:
    text = f"{req.title} {req.content}"
    keys = jieba.analyse.extract_tags(text, topK=top_k)
    return {"keywords": keys}


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest) -> AnalyzeResponse:
    cls = classify(req)
    kws = keywords(req, top_k=8)
    return AnalyzeResponse(
        predicted_category=cls["predicted_category"],
        confidence=cls["confidence"],
        keywords=kws["keywords"],
    )
