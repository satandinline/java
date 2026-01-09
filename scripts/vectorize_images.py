#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
图片向量化脚本
将crawled_images目录中的图片向量化并存储到向量数据库
"""
import os
import sys
from pathlib import Path
from typing import List, Dict
import base64
from PIL import Image
import io

# 添加项目根目录到路径（使用相对路径）
current_file_dir = os.path.dirname(os.path.realpath(__file__))
project_root = os.path.dirname(current_file_dir)
sys.path.insert(0, project_root)
sys.path.insert(0, os.path.join(project_root, 'AIGC'))

from dotenv import load_dotenv
load_dotenv(dotenv_path=os.path.join(project_root, '.env'))

# 导入必要的库
try:
    from langchain_community.vectorstores import Chroma
    from langchain_openai import OpenAIEmbeddings
    from langchain_community.embeddings import DashScopeEmbeddings
    from langchain.schema import Document
except ImportError as e:
    print(f"错误：缺少必要的库，请运行: pip install langchain langchain-openai langchain-community")
    sys.exit(1)

# 导入图片理解模型
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY") or os.getenv("ALIYUN_API_KEY")

def get_image_embedding_model():
    """获取图片嵌入模型"""
    # 优先使用支持图片的模型
    if DASHSCOPE_API_KEY:
        try:
            # 阿里云的多模态模型支持图片
            return DashScopeEmbeddings(
                dashscope_api_key=DASHSCOPE_API_KEY,
                model="text-embedding-v2"
            )
        except:
            pass
    
    if OPENAI_API_KEY:
        try:
            return OpenAIEmbeddings(model="text-embedding-3-large")
        except:
            pass
    
    raise Exception("未配置图片嵌入模型API密钥（需要OPENAI_API_KEY或DASHSCOPE_API_KEY）")

def describe_image_with_vision(image_path: str) -> str:
    """
    使用视觉模型描述图片内容
    """
    if not os.path.exists(image_path):
        return ""
    
    try:
        if OPENAI_API_KEY:
            from openai import OpenAI
            client = OpenAI(api_key=OPENAI_API_KEY)
            
            with open(image_path, "rb") as image_file:
                response = client.chat.completions.create(
                    model="gpt-4o",  # 使用新的模型
                    messages=[
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "请详细描述这张图片的内容，特别是与传统节日、文化、民俗相关的元素。包括：1. 图片中的主要元素（人物、物品、场景等）2. 传统节日相关的特征 3. 文化内涵和象征意义。用中文回答。"
                                },
                                {
                                    "type": "image_url",
                                    "image_url": {
                                        "url": f"file://{os.path.realpath(image_path)}"
                                    }
                                }
                            ],
                            "max_tokens": 500
                        }
                    ]
                )
                description = response.choices[0].message.content
                return description if description else ""
        
        elif DASHSCOPE_API_KEY:
            # 使用阿里云通义千问VL模型
            try:
                import dashscope
                from dashscope import MultiModalConversation
                dashscope.api_key = DASHSCOPE_API_KEY
                
                with open(image_path, 'rb') as f:
                    messages = [
                        {
                            "role": "user",
                            "content": [
                                {
                                    "image": f"file://{os.path.realpath(image_path)}"
                                },
                                {
                                    "text": "请详细描述这张图片的内容，特别是与传统节日、文化、民俗相关的元素。包括：1. 图片中的主要元素（人物、物品、场景等）2. 传统节日相关的特征 3. 文化内涵和象征意义。用中文回答。"
                                }
                            ]
                        }
                    ]
                    response = MultiModalConversation.call(model='qwen-vl-max', messages=messages)
                    if response.status_code == 200:
                        description = response.output.choices[0].message.content[0].get('text', '')
                        return description if description else ""
            except Exception as e:
                print(f"使用阿里云模型描述图片失败: {e}")
        
        return f"图片文件: {os.path.basename(image_path)}"
    except Exception as e:
        print(f"描述图片失败 {image_path}: {e}")
        return f"图片文件: {os.path.basename(image_path)}"

def vectorize_images(crawled_images_dir: str, persist_directory: str, batch_size: int = 10):
    """
    将crawled_images目录中的图片向量化并存储
    
    :param crawled_images_dir: crawled_images目录路径
    :param persist_directory: 向量数据库持久化目录
    :param batch_size: 批处理大小
    """
    print("=" * 60)
    print("开始图片向量化处理")
    print("=" * 60)
    
    # 检查目录
    if not os.path.exists(crawled_images_dir):
        print(f"[ERROR] 目录不存在: {crawled_images_dir}")
        return False
    
    # 获取所有图片文件
    image_extensions = {'.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'}
    image_files = []
    for root, dirs, files in os.walk(crawled_images_dir):
        for file in files:
            if any(file.lower().endswith(ext) for ext in image_extensions):
                image_files.append(os.path.join(root, file))
    
    if not image_files:
        print(f"[WARN] 在 {crawled_images_dir} 中未找到图片文件")
        return False
    
    print(f"[INFO] 找到 {len(image_files)} 张图片")
    
    # 初始化向量数据库
    try:
        embedding_model = get_image_embedding_model()
        vector_store = Chroma(
            persist_directory=persist_directory,
            embedding_function=embedding_model
        )
        print(f"[INFO] 向量数据库初始化成功: {persist_directory}")
    except Exception as e:
        print(f"[ERROR] 向量数据库初始化失败: {e}")
        return False
    
    # 检查已处理的图片（避免重复处理）
    processed_images = set()
    try:
        # 从向量库中获取已存在的图片路径
        existing_docs = vector_store.get()
        if existing_docs and 'metadatas' in existing_docs:
            for metadata in existing_docs.get('metadatas', []):
                if 'image_path' in metadata:
                    processed_images.add(metadata['image_path'])
        print(f"[INFO] 已处理 {len(processed_images)} 张图片，跳过重复处理")
    except:
        pass
    
    # 处理图片
    success_count = 0
    fail_count = 0
    new_count = 0
    
    for idx, image_path in enumerate(image_files, 1):
        # 使用相对路径作为唯一标识
        rel_path = os.path.relpath(image_path, project_root)
        
        # 检查是否已处理
        if image_path in processed_images or rel_path in processed_images:
            print(f"[{idx}/{len(image_files)}] 跳过已处理: {os.path.basename(image_path)}")
            continue
        
        try:
            print(f"[{idx}/{len(image_files)}] 处理: {os.path.basename(image_path)}")
            
            # 使用视觉模型描述图片
            description = describe_image_with_vision(image_path)
            if not description:
                description = f"图片: {os.path.basename(image_path)}"
            
            # 创建文档
            doc = Document(
                page_content=description,
                metadata={
                    'image_path': rel_path,
                    'absolute_path': image_path,
                    'filename': os.path.basename(image_path),
                    'source': 'crawled_images',
                    'type': 'image'
                }
            )
            
            # 添加到向量库
            vector_store.add_documents([doc])
            vector_store.persist()
            
            success_count += 1
            new_count += 1
            print(f"  [OK] 成功: {description[:50]}...")
            
            # 批处理时稍作延迟，避免API限流
            if idx % batch_size == 0:
                print(f"[INFO] 已处理 {idx} 张，暂停1秒...")
                import time
                time.sleep(1)
                
        except Exception as e:
            fail_count += 1
            print(f"  [ERROR] 失败: {e}")
    
    print("\n" + "=" * 60)
    print("图片向量化完成")
    print("=" * 60)
    print(f"总计: {len(image_files)} 张")
    print(f"新增: {new_count} 张")
    print(f"成功: {success_count} 张")
    print(f"失败: {fail_count} 张")
    print(f"跳过: {len(image_files) - new_count} 张（已处理）")
    print(f"向量数据库: {persist_directory}")
    print("=" * 60)
    
    return True

if __name__ == '__main__':
    # 使用相对路径
    project_root = Path(__file__).parent.parent
    crawled_images_dir = project_root / "crawled_images"
    persist_directory = str(project_root / "chroma_db_image")
    
    # 确保目录存在
    os.makedirs(persist_directory, exist_ok=True)
    
    print(f"项目根目录: {project_root}")
    print(f"图片目录: {crawled_images_dir}")
    print(f"向量数据库: {persist_directory}")
    print()
    
    success = vectorize_images(
        crawled_images_dir=str(crawled_images_dir),
        persist_directory=persist_directory,
        batch_size=10
    )
    
    if success:
        print("\n[OK] 图片向量化完成！")
        sys.exit(0)
    else:
        print("\n[ERROR] 图片向量化失败！")
        sys.exit(1)
