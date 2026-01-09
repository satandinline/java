# -*- coding: utf-8 -*-
"""
环境变量加载工具
提供统一的.env文件查找和加载功能
"""
import os
from dotenv import load_dotenv

def find_env_file(start_dir):
    """向上查找.env文件，直到找到或到达文件系统根目录"""
    current = os.path.abspath(start_dir)
    while True:
        env_path = os.path.join(current, '.env')
        if os.path.exists(env_path):
            return env_path
        parent = os.path.dirname(current)
        if parent == current:  # 已到达文件系统根目录
            break
        current = parent
    return None

def load_env_from_root(current_file_path):
    """
    从项目根目录加载.env文件
    项目根目录是指包含backend-python和backend-java目录的父目录
    
    Args:
        current_file_path: 当前文件的路径（通常使用__file__）
    
    Returns:
        str: .env文件的路径，如果找到的话
    """
    current_file_dir = os.path.dirname(os.path.realpath(current_file_path))
    # 先尝试从当前文件所在目录向上查找
    env_path = find_env_file(current_file_dir)
    if env_path:
        load_dotenv(dotenv_path=env_path, override=True)
        return env_path
    
    # 如果找不到，尝试找到项目根目录（包含backend-python或backend-java的目录）
    # 向上查找，直到找到包含backend-python或backend-java目录的父目录
    current = os.path.abspath(current_file_dir)
    while True:
        # 检查当前目录是否包含backend-python或backend-java
        backend_python = os.path.join(current, 'backend-python')
        backend_java = os.path.join(current, 'backend-java')
        if os.path.exists(backend_python) or os.path.exists(backend_java):
            # 找到了项目根目录
            env_path = os.path.join(current, '.env')
            if os.path.exists(env_path):
                load_dotenv(dotenv_path=env_path, override=True)
                return env_path
            # 即使.env不存在，也加载（可能后续会创建）
            load_dotenv(dotenv_path=env_path, override=True)
            return env_path
        
        parent = os.path.dirname(current)
        if parent == current:  # 已到达文件系统根目录
            break
        current = parent
    
    # 如果还是找不到，尝试从当前目录的父目录加载（向后兼容）
    project_root = os.path.dirname(current_file_dir)
    env_path = os.path.join(project_root, '.env')
    load_dotenv(dotenv_path=env_path, override=True)
    return env_path

