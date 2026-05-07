# Time Grid 原型更新说明

## 更新内容

由于原型文件过大，无法直接通过工具更新。请手动进行以下修改：

### 1. 登录页面 (login.html)
✅ 已创建完成：`docs/prototypes/login.html`

功能：
- 用户登录界面
- 登录成功后跳转到 time-grid-v1.html
- 将用户名和 token 存储到 localStorage

### 2. Time Grid 主页需要的更新

#### 2.1 Header 部分更新
将原来的多用户切换按钮替换为：
```html
<div class="flex items-center gap-3">
    <span class="text-sm text-gray-600">当前用户：<span class="font-medium text-gray-900">{{ currentUser }}</span></span>
    <button @click="showCategoryManager = true" class="px-4 py-2 bg-white border border-gray-300 rounded-lg font-medium text-gray-700 hover:bg-gray-50">
        管理分类
    </button>
    <button @click="logout" class="px-4 py-2 bg-white border border-gray-300 rounded-lg font-medium text-gray-700 hover:bg-gray-50">
        退出登录
    </button>
</div>
```

#### 2.2 Data 部分更新
```javascript
data() {
    return {
        currentUser: localStorage.getItem('username') || 'Demo User',
        categories: [
            { name: '工作', colorCode: '#3b82f6' },
            { name: '学习', colorCode: '#8b5cf6' },
            // ... 其他分类，添加 colorCode 字段
        ],
        showCategoryManager: false,
        newCategoryName: '',
        newCategoryColor: '#3b82f6',
        editingCategory: null,
        editCategoryName: '',
        editCategoryColor: '',
        records: [ /* 直接使用数组，不再按用户分组 */ ],
        // ... 其他字段保持不变
    };
}
```

#### 2.3 添加分类管理模态框
在 `</div>` (app 结束标签) 之前添加分类管理模态框的 HTML。

#### 2.4 添加新方法
```javascript
methods: {
    // ... 保留原有方法
    
    logout() {
        if (confirm('确定要退出登录吗？')) {
            localStorage.removeItem('username');
            localStorage.removeItem('token');
            window.location.href = 'login.html';
        }
    },
    
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
            name: this.newCategoryName.trim(),
            colorCode: this.newCategoryColor
        });
        this.newCategoryName = '';
        this.newCategoryColor = '#3b82f6';
    },
    
    startEditCategory(cat) {
        this.editingCategory = cat.name;
        this.editCategoryName = cat.name;
        this.editCategoryColor = cat.colorCode;
    },
    
    saveEditCategory(oldName) {
        if (!this.editCategoryName.trim()) {
            alert('请输入分类名称');
            return;
        }
        const cat = this.categories.find(c => c.name === oldName);
        if (cat) {
            cat.name = this.editCategoryName.trim();
            cat.colorCode = this.editCategoryColor;
            this.records.forEach(record => {
                if (record.category === oldName) {
                    record.category = cat.name;
                }
            });
        }
        this.editingCategory = null;
    },
    
    cancelEditCategory() {
        this.editingCategory = null;
    },
    
    deleteCategory(categoryName) {
        const inUse = this.records.some(r => r.category === categoryName);
        if (inUse) {
            alert('该分类正在被使用，无法删除。\n\n请先删除或修改使用该分类的时间记录。');
            return;
        }
        if (confirm(`确定要删除分类"${categoryName}"吗？`)) {
            const index = this.categories.findIndex(c => c.name === categoryName);
            if (index > -1) {
                this.categories.splice(index, 1);
            }
        }
    }
}
```

#### 2.5 添加登录检查
在 `mounted()` 中添加：
```javascript
mounted() {
    if (!localStorage.getItem('username')) {
        window.location.href = 'login.html';
    }
    document.addEventListener('mouseup', this.handleMouseUp);
}
```

#### 2.6 移除多用户切换功能
删除 `switchUser()` 方法和 `users` 数据字段。

## 完整原型文件位置

- `docs/prototypes/login.html` - ✅ 已创建
- `docs/prototypes/time-grid-v1.html` - ⚠️ 需要手动更新

## 测试流程

1. 打开 `login.html`
2. 输入任意用户名和密码
3. 点击"登录"
4. 自动跳转到 `time-grid-v1.html`
5. 点击"管理分类"按钮，测试分类管理功能
6. 点击"退出登录"，返回登录页面
