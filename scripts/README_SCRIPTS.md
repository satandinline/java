# Scripts目录说明

本目录包含的工具脚本说明：

## AIGC相关脚本（保留）

以下脚本属于AIGC功能模块，使用Python实现是合理的：

- `vectorize_images.py` - 图片向量化脚本，用于多模态搜索功能，属于AIGC相关功能

## 工具脚本（保留）

以下脚本是通用的工具脚本，被AIGC功能使用：

- `db_connection.py` - 数据库连接工具，被AIGC功能使用
- `env_loader.py` - 环境变量加载工具，被`db_connection.py`使用
- `festival_name_utils.py` - 节日名称转换工具，被AIGC功能使用

## 注意事项

1. **AIGC功能**：AIGC相关的Python脚本保留在scripts目录中，因为AIGC功能本身使用Python实现
2. **Java实现**：除了AIGC功能外，所有业务功能都使用Java实现
3. **工具脚本**：通用的工具脚本（如数据库连接、环境变量加载、节日名称转换）保留，因为它们被AIGC功能使用
4. **冗余代码清理**：已删除`export_user_resource.py`，该功能已完全用Java实现（`ExportService.java`和`ExportController.java`）
