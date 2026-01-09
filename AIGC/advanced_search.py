#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
高级搜索功能增强模块
结合停用词表、同义词表和相似度算法，优化多模态搜索功能
"""

import os
import json
from typing import List, Dict, Tuple, Optional
from pathlib import Path
from search_optimizer import SearchOptimizer


class AdvancedSearchEnhancer:
    """
    高级搜索增强器
    集成停用词过滤、同义词扩展、相似度计算等功能
    """
    
    def __init__(self):
        """
        初始化高级搜索增强器
        """
        self.optimizer = SearchOptimizer()
        self.search_history = []
    
    def enhanced_search(self, query: str, documents: List[Dict], top_k: int = 10) -> List[Tuple[Dict, float]]:
        """
        执行增强搜索
        
        Args:
            query: 搜索查询
            documents: 文档列表，每个文档应包含id、content、title等字段
            top_k: 返回最相似的前k个结果
            
        Returns:
            排序后的结果列表，包含文档和相似度分数
        """
        if not query or not documents:
            return []
        
        # 预处理查询
        processed = self.optimizer.preprocess_search_query(query)
        
        results = []
        for doc in documents:
            content = doc.get('content', '') or doc.get('title', '') or ''
            
            # 计算原始查询相似度
            orig_sim = self.optimizer.calculate_similarity(query, content)
            
            # 计算扩展查询相似度
            max_exp_sim = 0
            for exp_query in processed['expanded_queries']:
                exp_sim = self.optimizer.calculate_similarity(exp_query, content)
                max_exp_sim = max(max_exp_sim, exp_sim)
            
            # 最终相似度取最大值
            final_sim = max(orig_sim, max_exp_sim)
            
            results.append((doc, final_sim))
        
        # 按相似度排序
        results.sort(key=lambda x: x[1], reverse=True)
        
        # 返回top_k结果
        return results[:top_k]
    
    def multimodal_search_enhancement(self, query: str, text_results: List[Dict], 
                                    image_results: List[Dict]) -> Dict[str, List]:
        """
        多模态搜索增强
        
        Args:
            query: 用户查询
            text_results: 文本搜索结果
            image_results: 图片搜索结果
            
        Returns:
            增强后的搜索结果
        """
        # 预处理查询
        processed = self.optimizer.preprocess_search_query(query)
        
        # 增强文本结果
        enhanced_text_results = []
        for item in text_results:
            content = item.get('content', '') or item.get('title', '') or ''
            similarity = self.optimizer.calculate_similarity(query, content)
            
            # 添加同义词扩展匹配
            for exp_query in processed['expanded_queries']:
                exp_similarity = self.optimizer.calculate_similarity(exp_query, content)
                similarity = max(similarity, exp_similarity)
            
            item['enhanced_similarity'] = similarity
            enhanced_text_results.append(item)
        
        # 增强图片结果
        enhanced_image_results = []
        for item in image_results:
            content = item.get('content', '') or item.get('description', '') or ''
            similarity = self.optimizer.calculate_similarity(query, content)
            
            # 添加同义词扩展匹配
            for exp_query in processed['expanded_queries']:
                exp_similarity = self.optimizer.calculate_similarity(exp_query, content)
                similarity = max(similarity, exp_similarity)
            
            item['enhanced_similarity'] = similarity
            enhanced_image_results.append(item)
        
        # 按相似度排序
        enhanced_text_results.sort(key=lambda x: x.get('enhanced_similarity', 0), reverse=True)
        enhanced_image_results.sort(key=lambda x: x.get('enhanced_similarity', 0), reverse=True)
        
        return {
            'text_results': enhanced_text_results,
            'image_results': enhanced_image_results,
            'query_expansions': processed['expanded_queries'],
            'processed_query': processed['cleaned_query']
        }
    
    def semantic_search(self, query: str, documents: List[Dict], threshold: float = 0.1) -> List[Dict]:
        """
        语义搜索 - 返回高于阈值的匹配结果
        
        Args:
            query: 搜索查询
            documents: 文档列表
            threshold: 相似度阈值
            
        Returns:
            符合条件的文档列表
        """
        if not query or not documents:
            return []
        
        # 预处理查询
        processed = self.optimizer.preprocess_search_query(query)
        
        results = []
        for doc in documents:
            content = doc.get('content', '') or doc.get('title', '') or ''
            
            # 计算原始查询相似度
            orig_sim = self.optimizer.calculate_similarity(query, content)
            
            # 计算扩展查询相似度
            max_exp_sim = 0
            for exp_query in processed['expanded_queries']:
                exp_sim = self.optimizer.calculate_similarity(exp_query, content)
                max_exp_sim = max(max_exp_sim, exp_sim)
            
            # 最终相似度取最大值
            final_sim = max(orig_sim, max_exp_sim)
            
            if final_sim >= threshold:
                doc['semantic_similarity'] = final_sim
                results.append(doc)
        
        # 按相似度排序
        results.sort(key=lambda x: x.get('semantic_similarity', 0), reverse=True)
        
        return results
    
    def get_search_statistics(self) -> Dict:
        """
        获取搜索统计信息
        
        Returns:
            搜索统计信息
        """
        return {
            'total_searches': len(self.search_history),
            'unique_queries': len(set(item['query'] for item in self.search_history)),
            'last_search': self.search_history[-1] if self.search_history else None
        }


# 与现有系统的集成示例
def integrate_with_existing_search():
    """
    与现有搜索系统的集成示例
    """
    enhancer = AdvancedSearchEnhancer()
    
    # 模拟现有系统的搜索结果
    sample_documents = [
        {
            'id': 1,
            'title': '春节的传统习俗',
            'content': '春节是中国最重要的传统节日，有着丰富的习俗，如贴春联、放鞭炮、吃饺子。',
            'type': 'text',
            'tags': ['春节', '传统', '习俗']
        },
        {
            'id': 2,
            'title': '中秋节的家庭团聚',
            'content': '中秋节是家人团聚的日子，有赏月、吃月饼的传统习俗。',
            'type': 'text',
            'tags': ['中秋', '团聚', '月饼']
        },
        {
            'id': 3,
            'title': '端午节的历史',
            'content': '端午节是为了纪念屈原，有赛龙舟、吃粽子的传统。',
            'type': 'text',
            'tags': ['端午', '屈原', '龙舟']
        },
        {
            'id': 4,
            'title': '传统手工艺品',
            'content': '中国传统手工艺品包括剪纸、刺绣、陶瓷等多种形式。',
            'type': 'text',
            'tags': ['手工艺', '传统', '文化']
        }
    ]
    
    # 测试搜索增强
    query = "新年的传统"
    print(f"搜索查询: {query}")
    
    # 使用增强搜索
    enhanced_results = enhancer.enhanced_search(query, sample_documents, top_k=5)
    
    print(f"\n增强搜索结果 (共{len(enhanced_results)}项):")
    for i, (doc, similarity) in enumerate(enhanced_results, 1):
        print(f"{i}. [相似度: {similarity:.2f}] {doc['title']}")
        print(f"   内容: {doc['content'][:100]}...")
    
    # 语义搜索
    semantic_results = enhancer.semantic_search(query, sample_documents, threshold=0.05)
    print(f"\n语义搜索结果 (阈值>0.05, 共{len(semantic_results)}项):")
    for i, doc in enumerate(semantic_results, 1):
        print(f"{i}. [相似度: {doc['semantic_similarity']:.2f}] {doc['title']}")
    
    return enhancer


if __name__ == "__main__":
    print("=" * 60)
    print("高级搜索功能增强模块演示")
    print("=" * 60)
    
    enhancer = integrate_with_existing_search()
    
    print("\n" + "=" * 60)
    print("搜索优化模块已准备好集成到现有系统中!")
    print("功能包括:")
    print("1. 增强搜索 - 结合同义词扩展提高召回率")
    print("2. 语义搜索 - 基于相似度过滤结果")
    print("3. 多模态搜索增强 - 统一处理文本和图片搜索")
    print("4. 搜索历史统计 - 追踪搜索效果")
    print("=" * 60)
