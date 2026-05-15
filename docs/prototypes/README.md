# 原型文件说明

> UI 原型文件和使用指南

---

## 📁 原型文件清单

### ✅ login.html
**状态**: 完整可用  
**功能**: 
- 用户登录界面
- 表单验证（用户名和密码必填）
- 登录成功后跳转到 time-grid-simple.html
- Token 存储到 localStorage

**测试方法**:
1. 双击打开 `login.html`
2. 输入任意用户名和密码（原型阶段，任意值都可以）
3. 点击"登录"按钮
4. 自动跳转到 Time Grid 页面

---

### ✅ time-grid-simple.html
**状态**: 完整可用（简化版）  
**功能**:
- 显示当前登录用户
- 退出登录功能
- 登录状态检查（未登录自动跳转到登录页）
- 显示开发进度

**说明**: 
这是一个简化版本，用于测试登录流程。完整的 96 格子功能将在后端 API 完成后开发。

---

### ⚠️ time-grid-v1.html
**状态**: 文件损坏（被覆盖为 TodoMVC 示例）  
**问题**: 在尝试创建文件时，意外下载了一个 TodoMVC 示例覆盖了原文件

**解决方案**: 
- 当前使用 `time-grid-simple.html` 作为临时替代
- 完整版本将在后端 API 完成后重新开发

---

## 🎯 可测试的功能

### 1. 登录流程测试
```
打开 login.html 
  ↓
输入用户名和密码
  ↓
点击登录
  ↓
跳转到 time-grid-simple.html
  ↓
显示欢迎页面
```

### 2. 退出登录测试
```
在 time-grid-simple.html 页面
  ↓
点击"退出登录"按钮
  ↓
确认退出
  ↓
跳转回 login.html
```

### 3. 登录状态检查
```
直接打开 time-grid-simple.html
  ↓
检测到未登录
  ↓
自动跳转到 login.html
```

---

## 📋 完整功能开发计划

### 阶段 1: 后端实现（当前阶段）
- [x] 认证模块设计和测试
- [x] 认证模块实现
- [ ] 分类管理模块实现
- [ ] 时间块管理模块实现
- [ ] 运行所有测试
- [ ] 启动后端服务

### 阶段 2: 前端完整原型
- [ ] 重建 time-grid-v1.html
- [ ] 实现 96 格子网格（24小时 × 4格/小时）
- [ ] 实现拖拽选择时间块
- [ ] 实现分类管理模态框
- [ ] 实现时间块 CRUD 操作

### 阶段 3: 前后端集成
- [ ] 连接真实 API
- [ ] 替换 localStorage 为 API 调用
- [ ] 实现 JWT 认证
- [ ] 测试完整流程

---

## 🔧 完整版 Time Grid 功能需求

### 核心功能
1. **96 格子时间网格**
   - 24 小时（0:00 - 23:45）
   - 每格 15 分钟
   - 横向布局，每行 12 格（3 小时）

2. **时间块选择**
   - 点击单个格子
   - 拖拽选择多个连续格子
   - 显示选中状态

3. **分类管理**
   - 添加新分类（名称 + 颜色）
   - 编辑分类
   - 删除分类（检查是否在使用中）
   - 分类列表显示

4. **时间块操作**
   - 创建：选择时间范围 + 选择分类
   - 编辑：修改分类或备注
   - 删除：移除时间块
   - 显示：格子显示对应分类颜色

5. **用户功能**
   - 显示当前登录用户
   - 退出登录
   - 登录状态检查

### Header 部分设计
```html
<div class="flex items-center justify-between">
    <div>
        <h1 class="text-3xl font-bold text-gray-900">Time Grid</h1>
        <p class="text-gray-600 mt-1">以 15 分钟为单位记录今天做了什么</p>
    </div>
    <div class="flex items-center gap-3">
        <span class="text-sm text-gray-600">
            当前用户：<span class="font-medium text-gray-900">{{ currentUser }}</span>
        </span>
        <button @click="showCategoryManager = true" 
            class="px-4 py-2 bg-white border border-gray-300 rounded-lg">
            管理分类
        </button>
        <button @click="logout" 
            class="px-4 py-2 bg-white border border-gray-300 rounded-lg">
            退出登录
        </button>
    </div>
</div>
```

### 数据结构设计
```javascript
data() {
    return {
        currentUser: localStorage.getItem('username') || 'Demo User',
        
        // 分类列表
        categories: [
            { id: 1, name: '工作', colorCode: '#3b82f6' },
            { id: 2, name: '学习', colorCode: '#8b5cf6' },
            { id: 3, name: '运动', colorCode: '#10b981' },
            // ...
        ],
        
        // 时间块记录
        records: [
            {
                id: 1,
                startTime: '09:00',
                endTime: '10:30',
                categoryId: 1,
                note: '开发新功能'
            },
            // ...
        ],
        
        // 分类管理
        showCategoryManager: false,
        newCategoryName: '',
        newCategoryColor: '#3b82f6',
        editingCategory: null,
        
        // 时间块选择
        selecting: false,
        selectedSlots: [],
        
        // 其他状态
        showRecordModal: false,
        currentRecord: null,
    };
}
```

### 关键方法
```javascript
methods: {
    // 登录相关
    logout() {
        if (confirm('确定要退出登录吗？')) {
            localStorage.removeItem('username');
            localStorage.removeItem('token');
            window.location.href = 'login.html';
        }
    },
    
    // 分类管理
    addCategory() {
        if (!this.newCategoryName.trim()) {
            alert('请输入分类名称');
            return;
        }
        const exists = this.categories.find(c => c.name === this.newCategoryName.trim());
        if (exists) {
            alert('分类名称已存在');
            return;
        }
        this.categories.push({
            id: Date.now(),
            name: this.newCategoryName.trim(),
            colorCode: this.newCategoryColor
        });
        this.newCategoryName = '';
        this.newCategoryColor = '#3b82f6';
    },
    
    editCategory(category) {
        this.editingCategory = category;
        this.editCategoryName = category.name;
        this.editCategoryColor = category.colorCode;
    },
    
    saveCategory() {
        if (!this.editCategoryName.trim()) {
            alert('请输入分类名称');
            return;
        }
        this.editingCategory.name = this.editCategoryName.trim();
        this.editingCategory.colorCode = this.editCategoryColor;
        this.editingCategory = null;
    },
    
    deleteCategory(category) {
        const inUse = this.records.some(r => r.categoryId === category.id);
        if (inUse) {
            alert('该分类正在使用中，无法删除');
            return;
        }
        if (confirm(`确定要删除分类"${category.name}"吗？`)) {
            const index = this.categories.indexOf(category);
            this.categories.splice(index, 1);
        }
    },
    
    // 时间块操作
    startSelection(slotIndex) {
        this.selecting = true;
        this.selectedSlots = [slotIndex];
    },
    
    continueSelection(slotIndex) {
        if (!this.selecting) return;
        // 实现连续选择逻辑
    },
    
    endSelection() {
        if (this.selectedSlots.length > 0) {
            this.showRecordModal = true;
        }
        this.selecting = false;
    },
    
    saveRecord() {
        // 保存时间块记录
    },
    
    deleteRecord(record) {
        if (confirm('确定要删除这条记录吗？')) {
            const index = this.records.indexOf(record);
            this.records.splice(index, 1);
        }
    },
    
    // 工具方法
    getSlotColor(slotIndex) {
        const record = this.records.find(r => this.isSlotInRecord(slotIndex, r));
        if (record) {
            const category = this.categories.find(c => c.id === record.categoryId);
            return category ? category.colorCode : '#e5e7eb';
        }
        return '#ffffff';
    },
    
    isSlotInRecord(slotIndex, record) {
        // 判断格子是否在记录的时间范围内
    }
}
```

---

## 🔄 如何恢复完整的 Time Grid

### 方案 1: 从 Git 恢复（如果有提交）
```bash
cd D:\Project2\Interval
git log --oneline -- docs/prototypes/time-grid-v1.html
git checkout <commit-hash> -- docs/prototypes/time-grid-v1.html
```

### 方案 2: 等待后端完成后重新开发（推荐）
这是推荐方案，因为：
- 可以直接对接真实 API
- 避免重复工作
- 数据结构更准确
- 可以使用真实的用户和分类数据

### 方案 3: 手动重建
参考以下文档：
1. 本文档的功能需求和数据结构
2. `docs/design/system-design.md` - 第 15 章 UI 交互设计
3. 原始设计中的 96 格子布局

---

## 📞 当前状态总结

### 已完成
- ✅ 登录页面原型
- ✅ 简化版 Time Grid（测试用）
- ✅ 登录流程测试
- ✅ 退出登录功能
- ✅ 登录状态检查

### 待开发
- ⏳ 96 格子时间网格
- ⏳ 拖拽选择功能
- ⏳ 分类管理模态框
- ⏳ 时间块 CRUD 操作
- ⏳ 与后端 API 集成

### 建议
1. 当前使用 `time-grid-simple.html` 测试登录流程
2. 专注于后端 API 实现
3. 后端完成后再开发完整的前端原型
4. 使用 Vue 3 + TypeScript 开发正式前端项目

---

## 📝 文件对照表

| 文件名 | 状态 | 用途 | 说明 |
|--------|------|------|------|
| login.html | ✅ 可用 | 登录页面 | 完整功能 |
| time-grid-simple.html | ✅ 可用 | 简化版 Time Grid | 测试登录流程 |
| time-grid-v1.html | ⚠️ 损坏 | 完整版 Time Grid | 需重建 |

---

## 🎓 技术栈

### 当前原型
- **HTML5** - 页面结构
- **Tailwind CSS** - 样式框架（CDN）
- **Vue 3** - 前端框架（CDN）
- **localStorage** - 临时数据存储

### 正式前端项目
- **Vue 3** - Composition API
- **TypeScript** - 类型安全
- **Vite** - 构建工具
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Axios** - HTTP 客户端

---

**最后更新**: 2026-05-08  
**状态**: 登录流程可测试，完整功能待后端 API 完成后开发  
**下一步**: 实现后端分类管理和时间块管理 API
