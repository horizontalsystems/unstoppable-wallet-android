---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name:   Quantifying Fairness in LLMs Beyond Tokens: A Semantic and Statistical Perspective
Authors: Weijie Xu, Yiwen Wang, Chi Xue, Xiangkun Hu, Xi Fang, Guimin Dong, Chandan K. Reddy
Subjects: Subjects:
Computation and Language (cs.CL); Artificial Intelligence (cs.AI); Computers and Society (cs.CY)
Arxiv link: https://arxiv.org/abs/
Pdf link: https://arxiv.org/pdf/
Abstract
Large Language Models (LLMs) often generate responses with inherent biases, undermining their reliability in real-world applications. Existing evaluation methods often overlook biases in long-form responses and the intrinsic variability of LLM outputs. To address these challenges, we propose FiSCo(Fine-grained Semantic Computation), a novel statistical framework to evaluate group-level fairness in LLMs by detecting subtle semantic differences in long-form responses across demographic groups. Unlike prior work focusing on sentiment or token-level comparisons, FiSCo goes beyond surface-level analysis by operating at the claim level, leveraging entailment checks to assess the consistency of meaning across responses. We decompose model outputs into semantically distinct claims and apply statistical hypothesis testing to compare inter- and intra-group similarities, enabling robust detection of subtle biases. We formalize a new group counterfactual fairness definition and validate FiSCo on both synthetic and human-annotated datasets spanning gender, race, and age. Experiments show that FiSco more reliably identifies nuanced biases while reducing the impact of stochastic LLM variability, outperforming various evaluation metrics.
Title:
      Trustwor
description:
---

# My Agent

Describe what your agent does here...
