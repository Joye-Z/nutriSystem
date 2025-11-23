<template>
  <div>

    <div class="card" style="margin-bottom: 5px;">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input v-model="data.name" style="width: 100%" placeholder="请输入食谱名称查询"></el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="data.category" style="width: 100%" placeholder="分类" clearable>
            <el-option label="家常菜" value="家常菜"></el-option>
            <el-option label="健康餐" value="健康餐"></el-option>
            <el-option label="减脂餐" value="减脂餐"></el-option>
            <el-option label="素菜" value="素菜"></el-option>
            <el-option label="荤菜" value="荤菜"></el-option>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="data.difficulty" style="width: 100%" placeholder="难度" clearable>
            <el-option label="简单" value="简单"></el-option>
            <el-option label="中等" value="中等"></el-option>
            <el-option label="困难" value="困难"></el-option>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="info" style="margin-left: 10px" @click="reset">重置</el-button>
        </el-col>
      </el-row>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <div style="margin-bottom: 10px" v-if="data.userRole === 'ADMIN'">
        <el-button type="primary" @click="handleAdd">新增食谱</el-button>
      </div>
      <el-table :data="data.tableData" stripe @row-click="handleRowClick">
        <el-table-column label="食谱图片" width="100">
          <template #default="scope">
            <el-image 
              :src="preprocessImageUrl(scope.row.image)" 
              style="width: 60px; height: 60px; border-radius: 6px" 
              :fallback="defaultImage" 
              @error="handleImageError($event, scope.row)"
            ></el-image>
          </template>
        </el-table-column>
        <el-table-column label="食谱名称" prop="name" width="200"></el-table-column>
        <el-table-column label="描述" prop="description" show-overflow-tooltip></el-table-column>
        <el-table-column label="分类" prop="category" width="100"></el-table-column>
        <el-table-column label="难度" prop="difficulty" width="80"></el-table-column>
        <el-table-column label="烹饪时间" width="100">
          <template #default="scope">
            {{ scope.row.cookingTime }}分钟
          </template>
        </el-table-column>
        <el-table-column label="热量" width="80">
          <template #default="scope">
            {{ scope.row.calories }}卡
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" v-if="data.userRole === 'ADMIN'">
          <template #default="scope">
            <el-button type="warning" size="small" @click="handleEdit(scope.row, $event)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row.id, $event)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <el-pagination background layout="prev, pager, next" v-model:page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total"/>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="data.form.id ? '编辑食谱' : '新增食谱'" width="50%" v-model="data.formVisible" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="data.form" label-width="100px" style="padding-right: 50px">
        <el-form-item label="食谱图片" prop="image">
          <div style="display: flex; align-items: center; gap: 20px;">
            <div v-if="data.form.image" style="flex-shrink: 0; position: relative;">
              <el-image 
                :src="preprocessImageUrl(data.form.image)" 
                style="width: 120px; height: 120px; border-radius: 8px; border: 1px solid #eee;" 
                :fallback="defaultImage"
                @error="handleImageError($event)"
              ></el-image>
              <el-button 
                type="danger" 
                size="small" 
                circle 
                style="position: absolute; top: -8px; right: -8px; padding: 4px;"
                @click="deleteImage"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <div style="flex: 1;">
              <el-upload :action="uploadUrl" list-type="picture" :on-success="handleImgSuccess" :show-file-list="false">
                <el-button type="primary">上传图片</el-button>
              </el-upload>
              <div style="margin-top: 10px; color: #999; font-size: 12px;">
                支持 JPG、PNG 格式，文件大小不超过 2MB
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="食谱名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="data.form.description" autocomplete="off" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="data.form.category" style="width: 100%">
                <el-option label="家常菜" value="家常菜"></el-option>
                <el-option label="健康餐" value="健康餐"></el-option>
                <el-option label="减脂餐" value="减脂餐"></el-option>
                <el-option label="素菜" value="素菜"></el-option>
                <el-option label="荤菜" value="荤菜"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="data.form.difficulty" style="width: 100%">
                <el-option label="简单" value="简单"></el-option>
                <el-option label="中等" value="中等"></el-option>
                <el-option label="困难" value="困难"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="烹饪时间" prop="cookingTime">
              <el-input v-model="data.form.cookingTime" type="number">
                <template #append>分钟</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="热量" prop="calories">
              <el-input v-model="data.form.calories" type="number">
                <template #append>卡路里</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="蛋白质" prop="protein">
              <el-input v-model="data.form.protein" type="number">
                <template #append>g</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="碳水化合物" prop="carbohydrate">
              <el-input v-model="data.form.carbohydrate" type="number">
                <template #append>g</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="脂肪" prop="fat">
              <el-input v-model="data.form.fat" type="number">
                <template #append>g</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="食材清单" prop="ingredients">
          <el-input type="textarea" :rows="3" v-model="data.form.ingredients" placeholder="每行一个食材，格式：食材名称 用量" autocomplete="off" />
        </el-form-item>
        <el-form-item label="烹饪步骤" prop="steps">
          <el-input type="textarea" :rows="4" v-model="data.form.steps" placeholder="每行一个步骤" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="data.formVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">保 存</el-button>
      </span>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="食谱详情" width="60%" v-model="data.detailVisible" :close-on-click-modal="false">
      <div v-if="data.currentRecipe" style="padding: 20px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-image 
              :src="preprocessImageUrl(data.currentRecipe.image)" 
              style="width: 100%; border-radius: 8px" 
              :fallback="defaultImage"
              @error="handleImageError($event)"
            ></el-image>
          </el-col>
          <el-col :span="16">
            <h2 style="margin-top: 0">{{ data.currentRecipe.name }}</h2>
            <p style="color: #666; line-height: 1.6">{{ data.currentRecipe.description }}</p>
            
            <el-divider></el-divider>
            
            <el-row :gutter="20">
              <el-col :span="6">
                <div style="text-align: center">
                  <div style="font-size: 24px; color: #409EFF; font-weight: bold">{{ data.currentRecipe.cookingTime }}</div>
                  <div style="color: #999">分钟</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div style="text-align: center">
                  <div style="font-size: 24px; color: #67C23A; font-weight: bold">{{ data.currentRecipe.calories }}</div>
                  <div style="color: #999">卡路里</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div style="text-align: center">
                  <div style="font-size: 24px; color: #E6A23C; font-weight: bold">{{ data.currentRecipe.protein }}</div>
                  <div style="color: #999">蛋白质(g)</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div style="text-align: center">
                  <div style="font-size: 24px; color: #F56C6C; font-weight: bold">{{ data.currentRecipe.difficulty }}</div>
                  <div style="color: #999">难度</div>
                </div>
              </el-col>
            </el-row>

            <el-divider></el-divider>
            
            <h3>食材清单</h3>
            <div style="background: #f8f9fa; padding: 15px; border-radius: 6px">
              <div v-for="(item, index) in data.currentRecipe.ingredients.split('\n')" :key="index" style="margin-bottom: 5px">
                <el-tag type="success" size="small">{{ index + 1 }}</el-tag>
                <span style="margin-left: 10px">{{ item }}</span>
              </div>
            </div>

            <el-divider></el-divider>
            
            <h3>烹饪步骤</h3>
            <div style="background: #f8f9fa; padding: 15px; border-radius: 6px">
              <div v-for="(step, index) in data.currentRecipe.steps.split('\n')" :key="index" style="margin-bottom: 15px">
                <div style="display: flex; align-items: flex-start">
                  <div style="background: #409EFF; color: white; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 10px; flex-shrink: 0">
                    {{ index + 1 }}
                  </div>
                  <div style="line-height: 1.6">{{ step }}</div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.detailVisible = false">关 闭</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import request from "@/utils/request";
import {reactive, ref, onMounted} from "vue";
import {ElMessageBox, ElMessage} from "element-plus";
import {Close} from "@element-plus/icons-vue";

// 默认错误图片URL
const defaultImage = '/images/default-image.png';

// 预处理图片URL，确保正确显示
const preprocessImageUrl = (url) => {
  if (!url) return '';
  
  // 移除URL中的查询参数部分（如果有）
  let cleanUrl = url;
  const queryParamIndex = url.indexOf('?');
  if (queryParamIndex > 0) {
    cleanUrl = url.substring(0, queryParamIndex);
  }
  
  // 直接返回原始URL（可能是OSS URL），不做代理转换
  return cleanUrl;
};

// 文件上传的接口地址
const uploadUrl = import.meta.env.VITE_BASE_URL + '/files/upload'

const data = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  detailVisible: false,
  currentRecipe: null,
  form: {},
  tableData: [],
  name: null,
  category: null,
  difficulty: null,
  userRole: JSON.parse(localStorage.getItem('system-user') || '{}').role || 'USER'
})

// 处理图片加载错误
const handleImageError = (event, row = null) => {
  console.error('图片加载失败:', event);
  const target = event.target;
  
  // 如果有row对象，记录失败信息但直接使用默认图片
  if (row && row.image) {
    console.warn(`食谱ID: ${row.id}, 图片URL: ${row.image} 加载失败`);
  }
  
  // 直接使用默认图片，不尝试转换URL
  if (target.src !== defaultImage) {
    console.log('使用默认图片');
    target.src = defaultImage;
  }
};

// 在组件挂载时自动调用load函数
onMounted(() => {
  console.log('组件挂载完成，自动调用load函数');
  load();
});

// 新增
const handleAdd = () => {
  data.form = {}
  data.formVisible = true
}

// 行点击事件
const handleRowClick = (row) => {
  data.currentRecipe = JSON.parse(JSON.stringify(row))
  data.detailVisible = true
}

// 编辑
const handleEdit = (row, event) => {
  if (event) {
    event.stopPropagation() // 阻止事件冒泡
  }
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

// 新增保存
const add = () => {
  request.post('/recipe/add', data.form).then(res => {
    if (res.code === '200') {
      // 先关闭弹窗，再加载数据，确保响应式更新
      data.formVisible = false
      // 强制刷新数据，确保获取最新列表
      setTimeout(() => {
        // 重置为第一页以确保能看到新添加的数据
        data.pageNum = 1
        load()
      }, 100)
      ElMessage.success('操作成功')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 编辑保存
const update = () => {
  request.put('/recipe/update', data.form).then(res => {
    if (res.code === '200') {
      // 先关闭弹窗，再加载数据，确保响应式更新
      data.formVisible = false
      // 强制刷新数据，确保获取最新列表
      setTimeout(() => {
        // 保持当前页码，但重新加载数据
        load()
      }, 100)
      ElMessage.success('操作成功')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 弹窗保存
const save = () => {
  // data.form有id就是更新，没有就是新增
  data.form.id ? update() : add()
}

// 删除
const handleDelete = (id, event) => {
  if (event) {
    event.stopPropagation() // 阻止事件冒泡
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗?', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/recipe/delete/' + id).then(res => {
      if (res.code === '200') {
        // 强制刷新数据，确保获取最新列表
        setTimeout(() => {
          // 检查当前页面是否只有一条数据，如果是则返回上一页
          if (data.tableData.length === 1 && data.pageNum > 1) {
            data.pageNum--
          }
          load()
        }, 100)
        ElMessage.success('操作成功')
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {})
}

// 删除图片
const deleteImage = () => {
  data.form.image = null
}

// 重置
const reset = () => {
  data.name = null
  data.category = null
  data.difficulty = null
  load()
}

// 处理文件上传的钩子
const handleImgSuccess = (res) => {
  console.log('上传响应:', res);
  // 直接使用完整的OSS URL，不再转换为代理URL
  const ossUrl = res.data;
  console.log('使用完整OSS URL:', ossUrl);
  
  // 直接赋值，不做任何URL转换
  data.form.image = ossUrl;
  
  // 立即尝试加载图片来验证URL是否有效
  const img = new Image();
  img.onload = () => console.log('图片URL有效，能够正常加载');
  img.onerror = () => console.error('图片URL无效，无法加载图片');
  img.src = ossUrl;
  
  ElMessage.success('上传成功')
}

// 从后端API获取实际数据
const load = () => {
  console.log('开始加载食谱数据');
  
  // 构造查询参数
  const params = {
    pageNum: data.pageNum,
    pageSize: data.pageSize,
    name: data.name,
    category: data.category,
    difficulty: data.difficulty
  };
  
  console.log('发送API请求获取食谱数据，参数:', params);
  
  // 调用后端API获取实际数据 (使用selectPage而不是list)
  request.get('/recipe/selectPage', { params }).then(res => {
    console.log('API响应完整内容:', JSON.stringify(res, null, 2));
    
    if (res.code === '200' && res.data) {
      // 详细记录响应数据结构
      console.log('响应数据类型:', typeof res.data);
      console.log('响应数据属性:', Object.keys(res.data));
      
      // 后端返回的是res.data.list，需要适配数据结构
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
      console.log('表格数据已设置，数量:', data.tableData.length);
      
      // 记录每个食谱的详细信息以便调试
      if (data.tableData.length > 0) {
        console.log('获取到的前5条食谱数据示例:');
        data.tableData.slice(0, 5).forEach(item => {
          console.log(`食谱ID: ${item.id}, 名称: ${item.name}, 图片URL: ${item.image}`);
        });
      } else {
        console.log('未获取到任何食谱数据');
      }
    } else {
      console.error('获取数据失败，响应码:', res.code, '响应消息:', res.msg || res.message);
      data.tableData = [];
      data.total = 0;
    }
  }).catch(error => {
    console.error('API请求失败，错误详情:', error);
    if (error.response) {
      console.error('服务器响应状态:', error.response.status);
      console.error('服务器响应数据:', error.response.data);
    }
    data.tableData = [];
    data.total = 0;
  });
}
</script>

<style scoped>
/* 可以添加一些自定义样式来美化图片加载错误的显示 */
.image-error-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
}
</style>