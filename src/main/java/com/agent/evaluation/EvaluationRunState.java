package com.agent.evaluation;

/** 持久化评测运行的有限状态。 */
public enum EvaluationRunState { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }
