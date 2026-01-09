#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
搜索优化工具类
提供停用词过滤、同义词扩展等功能，用于增强高级搜索功能
"""

import re
import os
from typing import List, Dict, Set
from pathlib import Path


class SearchOptimizer:
    """
    搜索优化器
    提供停用词过滤、同义词扩展等功能
    """
    
    def __init__(self, stopwords_path: str = None, synonyms_path: str = None):
        """
        初始化搜索优化器
        
        Args:
            stopwords_path: 停用词文件路径
            synonyms_path: 同义词文件路径
        """
        self.stopwords = set()
        self.synonyms_dict = {}
        
        # 默认路径
        base_dir = Path(__file__).parent
        if stopwords_path is None:
            stopwords_path = base_dir / "stopwords.txt"
        if synonyms_path is None:
            synonyms_path = base_dir / "synonyms.txt"
        
        self.load_stopwords(stopwords_path)
        self.load_synonyms(synonyms_path)
    
    def load_stopwords(self, file_path: str):
        """
        加载停用词表
        
        Args:
            file_path: 停用词文件路径
        """
        if not os.path.exists(file_path):
            print(f"警告: 停用词文件不存在: {file_path}")
            return
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        # 按行分割停用词（可能有多词在同一行）
                        words = [word.strip() for word in line.split(',') if word.strip()]
                        if words:
                            self.stopwords.update(words)
        except Exception as e:
            print(f"加载停用词文件失败: {e}")
    
    def load_synonyms(self, file_path: str):
        """
        加载同义词表
        
        Args:
            file_path: 同义词文件路径
        """
        if not os.path.exists(file_path):
            print(f"警告: 同义词文件不存在: {file_path}")
            return
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        # 按逗号分割同义词组
                        synonyms_group = [word.strip() for word in line.split(',') if word.strip()]
                        if len(synonyms_group) > 1:
                            for word in synonyms_group:
                                self.synonyms_dict[word] = synonyms_group
        except Exception as e:
            print(f"加载同义词文件失败: {e}")
    
    def remove_stopwords(self, text: str) -> str:
        """
        移除停用词
        
        Args:
            text: 输入文本
            
        Returns:
            移除停用词后的文本
        """
        if not text:
            return text
        
        # 简单的词语匹配和移除
        for stopword in self.stopwords:
            text = text.replace(stopword, '')
        
        # 清理多余的空格
        text = re.sub(r'\s+', ' ', text).strip()
        return text
    
    def tokenize_and_filter(self, text: str) -> List[str]:
        """
        分词并过滤停用词
        
        Args:
            text: 输入文本
            
        Returns:
            过滤后的词汇列表
        """
        if not text:
            return []
        
        # 简单的中文分词（按字符和常见词组）
        # 在实际应用中，可以使用jieba等专业分词工具
        import jieba
        tokens = list(jieba.cut(text))
        
        # 过滤停用词
        filtered_tokens = [token for token in tokens if token not in self.stopwords and token.strip()]
        return filtered_tokens
    
    def expand_query_with_synonyms(self, query: str) -> List[str]:
        """
        使用同义词扩展查询
        
        Args:
            query: 查询文本
            
        Returns:
            包含同义词扩展的查询列表
        """
        if not query:
            return [query]
        
        expanded_queries = set([query])
        
        # 检查查询中的每个词是否有同义词
        for word, synonyms in self.synonyms_dict.items():
            if word in query:
                for synonym in synonyms:
                    if synonym != word:
                        expanded_query = query.replace(word, synonym)
                        expanded_queries.add(expanded_query)
        
        return list(expanded_queries)
    
    def preprocess_search_query(self, query: str) -> Dict:
        """
        预处理搜索查询
        
        Args:
            query: 原始查询
            
        Returns:
            包含预处理结果的字典
        """
        if not query:
            return {
                'original_query': '',
                'cleaned_query': '',
                'tokens': [],
                'expanded_queries': [],
                'query_keywords': []
            }
        
        # 移除停用词
        cleaned_query = self.remove_stopwords(query)
        
        # 分词
        tokens = self.tokenize_and_filter(cleaned_query)
        
        # 扩展查询
        expanded_queries = self.expand_query_with_synonyms(query)
        
        # 获取关键词
        query_keywords = self.tokenize_and_filter(query)
        
        return {
            'original_query': query,
            'cleaned_query': cleaned_query,
            'tokens': tokens,
            'expanded_queries': expanded_queries,
            'query_keywords': query_keywords
        }
    
    def calculate_similarity(self, text1: str, text2: str) -> float:
        """
        计算两个文本的相似度（基于词汇重叠）
        
        Args:
            text1: 文本1
            text2: 文本2
            
        Returns:
            相似度分数 (0-1)
        """
        if not text1 or not text2:
            return 0.0
        
        # 预处理文本
        tokens1 = set(self.tokenize_and_filter(text1))
        tokens2 = set(self.tokenize_and_filter(text2))
        
        if not tokens1 and not tokens2:
            return 1.0
        if not tokens1 or not tokens2:
            return 0.0
        
        # 计算交集和并集
        intersection = tokens1.intersection(tokens2)
        union = tokens1.union(tokens2)
        
        # Jaccard相似度
        similarity = len(intersection) / len(union)
        return similarity


# 示例使用
if __name__ == "__main__":
    optimizer = SearchOptimizer()
    
    # 测试查询预处理
    test_query = "春节的传统习俗"
    result = optimizer.preprocess_search_query(test_query)
    
    print(f"原始查询: {result['original_query']}")
    print(f"清理后查询: {result['cleaned_query']}")
    print(f"分词结果: {result['tokens']}")
    print(f"扩展查询: {result['expanded_queries']}")
    print(f"查询关键词: {result['query_keywords']}")
    
    # 测试相似度计算
    similarity = optimizer.calculate_similarity("春节的传统习俗", "新年的传统活动")
    print(f"相似度: {similarity:.2f}")
