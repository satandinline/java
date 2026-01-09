#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
高级搜索功能优化演示
展示如何使用搜索优化工具增强搜索功能
"""

from search_optimizer import SearchOptimizer


def demo_search_optimization():
    """
    演示搜索优化功能
    """
    print("=" * 60)
    print("高级搜索功能优化演示")
    print("=" * 60)
    
    # 初始化搜索优化器
    optimizer = SearchOptimizer()
    
    # 测试不同的查询
    test_queries = [
        "春节的传统习俗",
        "中秋节的由来",
        "传统手工艺品制作",
        "中国书法艺术",
        "京剧表演艺术"
    ]
    
    print("\n1. 查询预处理演示:")
    print("-" * 40)
    for query in test_queries:
        result = optimizer.preprocess_search_query(query)
        print(f"\n原始查询: {result['original_query']}")
        print(f"清理后: {result['cleaned_query']}")
        print(f"分词结果: {result['tokens']}")
        print(f"关键词: {result['query_keywords']}")
        print(f"扩展查询: {result['expanded_queries'][:3]}...")  # 只显示前3个
    
    print("\n\n2. 相似度计算演示:")
    print("-" * 40)
    test_pairs = [
        ("春节的传统习俗", "新年的传统活动"),
        ("中秋节的月饼", "八月十五的月饼"),
        ("传统手工艺品", "民间艺术品"),
        ("中国书法", "毛笔字艺术"),
        ("京剧表演", "传统戏曲演出")
    ]
    
    for text1, text2 in test_pairs:
        similarity = optimizer.calculate_similarity(text1, text2)
        print(f"'{text1}' vs '{text2}': 相似度 = {similarity:.2f}")
    
    print("\n\n3. 搜索结果增强演示:")
    print("-" * 40)
    
    # 模拟数据库中的内容
    sample_contents = [
        "春节是中国最重要的传统节日，有着丰富的习俗，如贴春联、放鞭炮、吃饺子。",
        "农历新年是中华民族的传统佳节，人们会团聚在一起庆祝。",
        "中秋节又称月饼节，是家人团聚的日子，有赏月、吃月饼的习俗。",
        "仲秋节历史悠久，体现了中国人对团圆的美好向往。",
        "端午节是为了纪念屈原，有赛龙舟、吃粽子的传统。",
        "传统手工艺品体现了中华文化的博大精深，如剪纸、刺绣等。",
        "民间艺术形式多样，包括书法、国画、戏曲等。",
        "中国书法艺术源远流长，是中华文化的重要组成部分。"
    ]
    
    search_query = "春节的传统"
    print(f"搜索查询: {search_query}")
    
    # 使用优化后的搜索
    processed = optimizer.preprocess_search_query(search_query)
    print(f"扩展查询: {processed['expanded_queries']}")
    
    print("\n匹配结果:")
    for i, content in enumerate(sample_contents, 1):
        # 使用多种方式计算相似度
        original_sim = optimizer.calculate_similarity(search_query, content)
        expanded_sims = []
        
        for exp_query in processed['expanded_queries']:
            sim = optimizer.calculate_similarity(exp_query, content)
            expanded_sims.append(sim)
        
        max_sim = max([original_sim] + expanded_sims)
        
        if max_sim > 0.1:  # 阈值
            print(f"{i}. 相似度: {max_sim:.2f} - {content}")


def enhance_multimodal_search():
    """
    演示如何将搜索优化集成到多模态搜索中
    """
    print("\n\n4. 多模态搜索增强演示:")
    print("-" * 40)
    
    optimizer = SearchOptimizer()
    
    # 模拟用户查询
    user_query = "中国的传统节日"
    
    # 预处理查询
    processed = optimizer.preprocess_search_query(user_query)
    
    print(f"用户查询: {user_query}")
    print(f"扩展查询: {processed['expanded_queries']}")
    
    # 模拟数据库内容（文本+图片描述）
    database_entries = [
        {"id": 1, "type": "text", "content": "春节是中国最重要的传统节日，有贴春联、放鞭炮、吃饺子等习俗", "image": "chunjie_festival.jpg"},
        {"id": 2, "type": "image", "content": "中秋节家庭团聚赏月图", "image": "zhongqiujie_family.jpg"},
        {"id": 3, "type": "text", "content": "端午节纪念屈原，有赛龙舟、吃粽子的传统", "image": "duanwujie_dragon_boat.jpg"},
        {"id": 4, "type": "text", "content": "清明节踏青扫墓的传统", "image": "qingming_qingming.jpg"},
        {"id": 5, "type": "image", "content": "元宵节观灯会场景", "image": "yuanxiao_lantern_festival.jpg"}
    ]
    
    # 使用增强的搜索算法
    results = []
    for entry in database_entries:
        content = entry['content']
        
        # 计算原始查询相似度
        orig_sim = optimizer.calculate_similarity(user_query, content)
        
        # 计算扩展查询相似度
        max_exp_sim = 0
        for exp_query in processed['expanded_queries']:
            exp_sim = optimizer.calculate_similarity(exp_query, content)
            max_exp_sim = max(max_exp_sim, exp_sim)
        
        # 最终相似度取最大值
        final_sim = max(orig_sim, max_exp_sim)
        
        if final_sim > 0.05:  # 设置阈值
            results.append((entry, final_sim))
    
    # 按相似度排序
    results.sort(key=lambda x: x[1], reverse=True)
    
    print(f"\n搜索结果 (共{len(results)}项):")
    for i, (entry, similarity) in enumerate(results, 1):
        print(f"{i}. [相似度: {similarity:.2f}] {entry['content']} (图片: {entry['image']})")


if __name__ == "__main__":
    demo_search_optimization()
    enhance_multimodal_search()
    
    print("\n" + "=" * 60)
    print("搜索优化功能演示完成!")
    print("这些优化可以帮助改进高级搜索功能，包括:")
    print("1. 停用词过滤 - 提高搜索准确性")
    print("2. 同义词扩展 - 增加搜索召回率")
    print("3. 相似度计算 - 改进搜索排序")
    print("4. 查询预处理 - 优化搜索体验")
    print("=" * 60)
