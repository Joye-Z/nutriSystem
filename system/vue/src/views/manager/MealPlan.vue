<template>
  <div>
    <!-- 顶部功能栏 - 包含新增按钮、视图切换和搜索功能 -->
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; background-color: #f8f9fa; padding: 15px; border-radius: 8px;">
      <div style="display: flex; gap: 10px;">
        <el-button type="success" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增计划
        </el-button>
        <el-button @click="toggleViewMode">
          {{ viewMode === 'calendar' ? '列表视图' : '日历视图' }}
        </el-button>
      </div>
      
      <!-- 搜索功能 - 仅在列表视图下显示 -->
      <div v-if="viewMode !== 'calendar'" style="display: flex; align-items: center; gap: 10px;">
        <el-date-picker
          v-model="searchDate"
          type="date"
          placeholder="选择日期"
          @change="handleDateChange"
          style="width: 150px;"
        />
        <el-input
          v-model="searchForm.planName"
          placeholder="请输入计划名称"
          style="width: 200px;"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 日历视图 -->
    <div v-if="viewMode === 'calendar'">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <el-button @click="prevMonth">
          <el-icon><ArrowLeft /></el-icon>
          上个月
        </el-button>
        <h3 style="margin: 0;">{{ currentMonth }}</h3>
        <el-button @click="nextMonth">
          下个月
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      
      <div class="calendar-container">
        <div class="calendar-header">
          <div class="calendar-header-cell" v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day">
            {{ day }}
          </div>
        </div>
        <div class="calendar-body">
          <div 
            v-for="day in calendarDays" 
            :key="day.date"
            :class="['calendar-day', { 
              'current-month': day.isCurrentMonth, 
              'today': day.isToday,
              'has-plan': day.hasPlan
            }]"
            @click="viewDayPlan(day)"
          >
            <div class="day-number">{{ day.day }}</div>
            <div v-if="day.hasPlan" class="meal-indicators">
              <span v-for="mealType in day.mealTypes" :key="mealType" class="meal-indicator">
                {{ getMealTypeIcon(mealType) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <template #empty>
          <div style="text-align: center; padding: 50px; color: #999;">
            <el-icon size="50"><Document /></el-icon>
            <div style="margin-top: 20px;">暂无膳食计划数据</div>
            <div style="margin-top: 10px;">请点击"新增计划"按钮添加数据</div>
          </div>
        </template>
        <el-table-column prop="planDate" label="日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.planDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="planName" label="计划名称" width="150" />
        <el-table-column prop="mealType" label="餐次类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getMealTypeTag(row.mealType)">{{ row.mealType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recipeName" label="食谱" width="200">
          <template #default="{ row }">
            <!-- 选择了固定食谱时，只显示食谱名称 -->
            <div v-if="row.recipeId || row.recipe || row.recipeName">
              {{ getRecipeName(row.recipeId, row.recipe?.name, row.recipeName) }}
            </div>
            <!-- 未选择食谱时，显示自定义餐食内容 -->
            <div v-else>{{ row.customMeal || '自定义餐食' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="calories" label="热量(卡路里)" width="120" />
        <el-table-column prop="notes" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="计划日期" prop="planDate">
          <el-date-picker
            v-model="form.planDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="餐次类型" prop="mealType">
          <el-select v-model="form.mealType" placeholder="请选择餐次类型" style="width: 100%">
            <el-option label="早餐" value="早餐" />
            <el-option label="午餐" value="午餐" />
            <el-option label="晚餐" value="晚餐" />
            <el-option label="加餐" value="加餐" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择食谱" prop="recipeId">
          <el-select v-model="form.recipeId" placeholder="请选择食谱" style="width: 100%" filterable>
            <el-option 
              v-for="recipe in recipeList" 
              :key="recipe.id" 
              :label="recipe.name" 
              :value="recipe.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="自定义餐食" prop="customMeal">
          <el-input 
            v-model="form.customMeal" 
            type="textarea" 
            :rows="3" 
            placeholder="如不选择食谱，可在此输入自定义餐食内容"
          />
        </el-form-item>
        <el-form-item label="热量" prop="calories">
          <el-input-number 
            v-model="form.calories" 
            :min="0" 
            :max="5000" 
            placeholder="请输入热量"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input v-model="form.notes" type="textarea" :rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 日计划详情对话框 -->
    <el-dialog 
      v-model="dayPlanDialogVisible" 
      :title="`${selectedDay ? formatDetailDate(selectedDay.date) + ' 的膳食计划' : '日计划详情'}`" 
      width="900px"
      @close="closeDayPlanDialog"
    >
      <div v-if="selectedDay">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <h3 style="margin: 0;">
            {{ formatDetailDate(selectedDay.date) }} 的膳食计划
            <el-tag :type="dayPlanData.length > 0 ? 'success' : 'info'" style="margin-left: 10px;">
              {{ dayPlanData.length }} 个计划
            </el-tag>
          </h3>
          <div>
            <el-button type="primary" size="small" @click="handleAddForDate(selectedDay.date)">
              <el-icon><Plus /></el-icon>
              为该日添加计划
            </el-button>
          </div>
        </div>
        
        <div v-if="dayPlanData.length === 0" style="text-align: center; padding: 50px; color: #999;">
          <el-icon size="50" style="margin-bottom: 20px;"><Calendar /></el-icon>
          <div style="margin-bottom: 20px;">该日期暂无膳食计划</div>
          <el-button type="primary" @click="handleAddForDate(selectedDay.date)">
            <el-icon><Plus /></el-icon>
            添加第一个计划
          </el-button>
        </div>
        
        <el-table v-else :data="dayPlanData" stripe>
          <el-table-column prop="mealType" label="餐次类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getMealTypeTag(row.mealType)">{{ row.mealType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="recipeName" label="餐食内容" width="350">
            <template #default="{ row }">
              <!-- 选择了固定食谱时，只显示食谱名称 -->
              <div v-if="row.recipeId || row.recipe || row.recipeName">
                {{ getRecipeName(row.recipeId, row.recipe?.name, row.recipeName) }}
              </div>
              <!-- 未选择食谱时，显示自定义餐食内容 -->
              <div v-else>{{ row.customMeal || '自定义餐食' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="calories" label="热量" width="100">
            <template #default="{ row }">
              <el-tag type="warning" v-if="row.calories">{{ row.calories }} 卡</el-tag>
              <span v-else style="color: #999;">未设置</span>
            </template>
          </el-table-column>
          <el-table-column prop="notes" label="备注" show-overflow-tooltip />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 统计信息 -->
        <div v-if="dayPlanData.length > 0" style="margin-top: 20px; padding: 15px; background-color: #f5f7fa; border-radius: 8px;">
          <div style="display: flex; justify-content: space-around; text-align: center;">
            <div>
              <div style="font-size: 20px; font-weight: bold; color: #409eff;">{{ dayPlanData.length }}</div>
              <div style="color: #666; font-size: 12px;">总计划数</div>
            </div>
            <div>
              <div style="font-size: 20px; font-weight: bold; color: #67c23a;">
                {{ dayPlanData.reduce((sum, item) => sum + (item.calories || 0), 0) }}
              </div>
              <div style="color: #666; font-size: 12px;">总热量(卡)</div>
            </div>
            <div>
              <div style="font-size: 20px; font-weight: bold; color: #e6a23c;">
                {{ Math.round(dayPlanData.reduce((sum, item) => sum + (item.calories || 0), 0) / dayPlanData.length) || 0 }}
              </div>
              <div style="color: #666; font-size: 12px;">平均热量(卡)</div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { 
  Plus, 
  Search, 
  Calendar, 
  List, 
  ArrowLeft, 
  ArrowRight,
  Document 
} from '@element-plus/icons-vue'

// 响应式数据
const tableData = ref([])
const dayPlanData = ref([])
const recipeList = ref([])
const dialogVisible = ref(false)
const dayPlanDialogVisible = ref(false)
const formRef = ref()
const viewMode = ref('calendar') // calendar or list
const searchDate = ref('')
const selectedDay = ref(null)
const loading = ref(false) // 加载状态

// 分页参数
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  planName: '',
  planDate: '',
  mealType: ''
})

// 表单数据
const form = reactive({
  id: null,
  planName: '',
  planDate: '',
  mealType: '',
  recipeId: null,
  customMeal: '',
  calories: null,
  notes: ''
})

// 表单验证规则
const rules = reactive({
  planName: [
    { required: true, message: '请输入计划名称', trigger: 'blur' }
  ],
  planDate: [
    { required: true, message: '请选择计划日期', trigger: 'change' }
  ],
  mealType: [
    { required: true, message: '请选择餐次类型', trigger: 'change' }
  ]
})

// 当前月份
const currentMonth = ref('')
const currentDate = ref(new Date())

// 计算日历天数
const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDate = new Date(firstDay)
  startDate.setDate(startDate.getDate() - firstDay.getDay())
  
  const days = []
  for (let i = 0; i < 42; i++) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + i)
    
    const day = date.getDate()
    const isCurrentMonth = date.getMonth() === month
    const isToday = date.toDateString() === new Date().toDateString()
    
    // 修复日期比较逻辑，确保时区一致
    // 使用本地时间生成日期字符串，避免时区偏移
    const dateStr = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    
    // 统一处理日期比较逻辑，避免时区问题
    const normalizeDateStr = (dateString) => {
      if (!dateString) return ''
      // 统一处理为 YYYY-MM-DD 格式
      let normalized = dateString
      if (normalized.includes('T')) {
        normalized = normalized.split('T')[0]
      }
      return normalized
    }
    
    // 检查该日期是否有计划
    const hasPlan = tableData.value.some(item => {
      return normalizeDateStr(item.planDate) === dateStr
    })
    
    // 获取该日期的餐次类型
    const mealTypes = tableData.value
      .filter(item => {
        return normalizeDateStr(item.planDate) === dateStr
      })
      .map(item => item.mealType)
    
    days.push({
      date: dateStr,
      day,
      isCurrentMonth,
      isToday,
      hasPlan,
      mealTypes
    })
  }
  
  return days
})

// 生命周期
onMounted(() => {
  updateCurrentMonth()
  loadData()
  loadRecipes()
})

// 更新当前月份显示
const updateCurrentMonth = () => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth() + 1
  currentMonth.value = `${year}年${month}月`
}

// 加载数据
const loadData = async () => {
  console.log('🔄 加载数据开始，当前视图模式:', viewMode.value)
  
  // 防止重复加载
  if (loading.value) {
    console.log('⚠️ 数据正在加载中，跳过重复请求')
    return
  }
  
  // 设置加载状态
  loading.value = true
  
  try {
    const user = JSON.parse(localStorage.getItem('system-user') || '{}')
    
    // 确保用户ID存在
    if (!user.id) {
      console.error('用户ID不存在，请先登录')
      ElMessage.error('请先登录')
      return
    }
    
    console.log('当前视图模式:', viewMode.value)
    console.log('当前搜索条件:', searchForm)
    console.log('当前分页参数 - pageNum:', pageNum.value, 'pageSize:', pageSize.value)
    console.log('当前tableData长度:', tableData.value.length)
    
    // 构建API参数
    const baseParams = {
      userId: user.id
    }
    
    // 添加搜索条件
    // 确保计划名称筛选正确处理
    if (searchForm.planName && searchForm.planName.trim()) {
      baseParams.planName = searchForm.planName.trim()
      console.log('添加计划名称筛选条件:', baseParams.planName)
    }
    if (searchForm.planDate) {
      baseParams.planDate = searchForm.planDate
      console.log('添加日期筛选条件:', baseParams.planDate)
    }
    if (searchForm.mealType) {
      baseParams.mealType = searchForm.mealType
      console.log('添加餐次类型筛选条件:', baseParams.mealType)
    }
    
    // 如果是日历视图，获取所有数据（不分页）
    if (viewMode.value === 'calendar') {
      console.log('日历视图模式：获取所有数据（不分页）')
      
      // 清除分页参数，确保获取所有数据
      const params = { ...baseParams }
      console.log('日历视图API参数:', params)
      
      const res = await request.get('/mealPlan/selectByUser', { params })
      console.log('日历视图完整API响应:', res)
      
      // 统一处理API响应数据结构
      let actualData = []
      if (res && res.data) {
        // 分页响应结构：{ list: [], total: 100 }
        if (res.data.list && Array.isArray(res.data.list)) {
          actualData = res.data.list
          console.log('分页数据结构：使用list字段，数据长度:', actualData.length)
        } 
        // 非分页响应结构：直接返回数组
        else if (Array.isArray(res.data)) {
          actualData = res.data
          console.log('非分页数据结构：直接返回数组，数据长度:', actualData.length)
        }
        // 其他可能的响应结构
        else if (res.data.data && Array.isArray(res.data.data)) {
          actualData = res.data.data
          console.log('嵌套数据结构：使用data字段，数据长度:', actualData.length)
        }
      } else {
        console.log('❌ API响应无效:', res)
      }
      
      console.log('日历视图解析后的实际数据:', actualData)
      console.log('日历视图数据长度:', actualData.length)
      
      // 只有在数据有效时才更新
      if (actualData && actualData.length >= 0) {
        tableData.value = actualData
        total.value = actualData.length
        console.log('✅ 日历数据更新成功')
      } else {
        console.log('⚠️ 日历数据为空，保持原状')
      }
      
      // 强制刷新日历显示
      currentDate.value = new Date(currentDate.value.getTime())
      console.log('日历视图数据加载完成，强制刷新日历显示')
    } else {
      // 列表视图模式：使用分页查询
      console.log('列表视图模式：使用分页查询')
      
      // 添加分页参数
      const params = {
        ...baseParams,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
      
      console.log('列表视图API参数:', params)
      
      const res = await request.get('/mealPlan/selectByUser', { params })
      console.log('列表视图完整API响应:', res)
      
      // 统一处理分页响应的数据结构
      let actualData = []
      let actualTotal = 0
      
      if (res && res.data) {
        // 标准分页响应结构：{ list: [], total: 100 }
        if (res.data.list && Array.isArray(res.data.list)) {
          actualData = res.data.list
          actualTotal = res.data.total || 0
          console.log('标准分页响应结构，数据长度:', actualData.length, '总数:', actualTotal)
        } 
        // 向后兼容：如果直接返回数组，则认为是所有数据
        else if (Array.isArray(res.data)) {
          actualData = res.data
          actualTotal = res.data.length
          console.log('向后兼容：直接返回数组，数据长度:', actualData.length, '总数:', actualTotal)
        }
        // 其他可能的响应结构
        else if (res.data.data && Array.isArray(res.data.data)) {
          actualData = res.data.data
          actualTotal = res.data.total || res.data.data.length
          console.log('嵌套分页结构，数据长度:', actualData.length, '总数:', actualTotal)
        }
      } else {
        console.log('❌ API响应无效:', res)
      }
      
      console.log('列表视图解析后的实际数据:', actualData)
      console.log('列表视图数据长度:', actualData.length)
      console.log('列表视图总数:', actualTotal)
      
      // 只有在数据有效时才更新
      if (actualData && actualData.length >= 0) {
        tableData.value = actualData
        total.value = actualTotal
        console.log('✅ 列表数据更新成功')
      } else {
        console.log('⚠️ 列表数据为空，保持原状')
      }
      
      console.log('列表视图分页数据加载完成')
    }
    
    // 验证数据一致性
    console.log('=== 数据一致性验证 ===')
    console.log('当前视图模式:', viewMode.value)
    console.log('tableData 长度:', tableData.value.length)
    console.log('total 总数:', total.value)
    
    // 检查今日是否有计划
    const today = new Date()
    const todayStr = `${today.getFullYear()}-${(today.getMonth() + 1).toString().padStart(2, '0')}-${today.getDate().toString().padStart(2, '0')}`
    const hasTodayPlan = tableData.value.some(item => {
      if (!item.planDate) return false
      let itemDateStr = item.planDate
      if (itemDateStr.includes('T')) {
        itemDateStr = itemDateStr.split('T')[0]
      }
      return itemDateStr === todayStr
    })
    
    console.log('今日是否有计划:', hasTodayPlan)
    console.log('=== 验证完成 ===')
    
  } catch (error) {
    console.error('加载数据错误详情:', error)
    console.error('错误响应:', error.response)
    console.error('错误状态:', error.status)
    
    // 更详细的错误信息
    if (error.response) {
      ElMessage.error(`加载失败: ${error.response.status} ${error.response.statusText}`)
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查后端服务是否正常运行')
    } else {
      ElMessage.error('加载数据失败，请检查控制台日志')
    }
  } finally {
    // 无论成功失败，都关闭加载状态
    loading.value = false
  }
}

// 加载食谱列表
const loadRecipes = async () => {
  try {
    const res = await request.get('/recipe/selectAll')
    recipeList.value = res?.data?.list || res?.data || []
  } catch (error) {
    console.error('加载食谱列表失败:', error)
  }
}

// 切换视图模式
const toggleViewMode = async () => {
  console.log('切换视图模式，当前视图:', viewMode.value)
  console.log('切换前 tableData 长度:', tableData.value.length)
  console.log('当前搜索条件:', searchForm)
  
  // 切换视图模式
  const oldViewMode = viewMode.value
  viewMode.value = viewMode.value === 'calendar' ? 'list' : 'calendar'
  console.log('切换后视图:', viewMode.value)
  
  // 如果是从日历切换到列表，需要重新加载数据以获取分页数据
  if (oldViewMode === 'calendar' && viewMode.value === 'list') {
    console.log('从日历切换到列表，重新加载分页数据...')
    
    // 切换到列表视图时，只重置分页参数，保留搜索条件
    pageNum.value = 1
    
    // 如果没有搜索条件，保持现有的搜索条件
    console.log('列表视图搜索条件:', searchForm)
    
    await loadData()
    
    // 强制刷新列表视图显示
    console.log('列表视图数据重新加载完成')
  } 
  // 如果是从列表切换到日历，需要加载所有数据
  else if (oldViewMode === 'list' && viewMode.value === 'calendar') {
    console.log('从列表切换到日历，重新加载所有数据...')
    
    // 切换到日历视图，需要加载所有数据
    await loadData()
    
    // 强制刷新日历显示
    currentDate.value = new Date(currentDate.value.getTime())
    console.log('日历视图强制刷新完成')
  }
  
  console.log('切换后 tableData 长度:', tableData.value.length)
  
  // 验证数据一致性
  console.log('=== 数据一致性验证 ===')
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${(today.getMonth() + 1).toString().padStart(2, '0')}-${today.getDate().toString().padStart(2, '0')}`
  console.log('今日日期字符串:', todayStr)
  
  const hasTodayPlan = tableData.value.some(item => {
    if (!item.planDate) return false
    let itemDateStr = item.planDate
    if (itemDateStr.includes('T')) {
      itemDateStr = itemDateStr.split('T')[0]
    }
    return itemDateStr === todayStr
  })
  
  console.log('今日是否有计划:', hasTodayPlan)
  console.log('=== 验证完成 ===')
  
  console.log('视图切换完成，数据已同步')
}

// 日期变化处理
const handleDateChange = (date) => {
  if (date) {
    // 修复时区问题：使用本地时间格式化，避免时区偏移
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    searchForm.planDate = `${year}-${month}-${day}`
    
    console.log('选择的日期:', date, '格式化后的日期:', searchForm.planDate)
    
    // 选择日期后不立即筛选，只更新搜索条件，等待用户点击搜索按钮
  } else {
    searchForm.planDate = ''
    dayPlanDialogVisible.value = false
    // 清空日期时也不立即重新加载，等待用户点击搜索按钮
    console.log('日期已清空')
  }
}

// 显示指定日期的计划详情
const showDayPlanDetail = async (planDate) => {
  try {
    const user = JSON.parse(localStorage.getItem('system-user') || '{}')
    
    if (!user.id) {
      ElMessage.error('请先登录')
      return
    }
    
    console.log('查看指定日期计划:', planDate)
    
    // 统一的日期比较函数
    const normalizeDateStr = (dateString) => {
      if (!dateString) return ''
      let normalized = dateString
      if (normalized.includes('T')) {
        normalized = normalized.split('T')[0]
      }
      return normalized
    }
    
    // 从已有数据中查找该日期的计划
    const dayData = tableData.value.filter(item => {
      return normalizeDateStr(item.planDate) === planDate
    })
    
    console.log('找到的日期计划:', dayData)
    console.log('搜索的日期:', planDate, '数据长度:', dayData.length)
    
    if (dayData.length > 0) {
      // 创建选中日期对象
      selectedDay.value = {
        date: planDate,
        isCurrentMonth: true,
        hasPlan: true,
        mealTypes: dayData.map(item => item.mealType)
      }
      
      // 设置日计划数据
      dayPlanData.value = dayData
      dayPlanDialogVisible.value = true
      
      console.log('显示日期详情对话框')
    } else {
      // 如果没有找到计划，显示空的详情对话框
      selectedDay.value = {
        date: planDate,
        isCurrentMonth: true,
        hasPlan: false,
        mealTypes: []
      }
      
      dayPlanData.value = []
      dayPlanDialogVisible.value = true
      
      console.log('显示空日期详情对话框:', planDate)
      // 暂时不显示提示消息，让用户看到空状态
      // ElMessage.info(`${planDate} 暂无膳食计划`)
    }
  } catch (error) {
    console.error('显示日期计划详情失败:', error)
    ElMessage.error('获取日期计划详情失败')
  }
}

// 搜索
const handleSearch = async () => {
  console.log('执行搜索操作，搜索条件:', searchForm)
  pageNum.value = 1
  await loadData()
  
  // 列表视图下只筛选信息，不弹出详情对话框
  // 日历视图下也不弹出详情对话框
}

// 重置搜索
const handleReset = () => {
  searchForm.planName = ''
  searchForm.planDate = ''
  searchForm.mealType = ''
  searchDate.value = ''
  pageNum.value = 1
  dayPlanDialogVisible.value = false
  loadData()
}

// 新增计划
const handleAdd = () => {
  Object.keys(form).forEach(key => {
    form[key] = ''
  })
  form.id = null
  dialogVisible.value = true
}

// 编辑计划
const handleEdit = (row) => {
  console.log('编辑原始数据:', row)
  
  // 清空表单
  Object.keys(form).forEach(key => {
    form[key] = ''
  })
  
  // 正确映射数据到表单
  form.id = row.id
  form.planName = row.planName || ''
  form.planDate = row.planDate || ''
  form.mealType = row.mealType || ''
  form.recipeId = row.recipeId || null
  form.customMeal = row.customMeal || ''
  form.calories = row.calories || null
  form.notes = row.notes || ''
  
  // 如果有日期，转换为Date对象用于日期选择器
  if (form.planDate) {
    form.planDate = new Date(form.planDate)
  }
  
  console.log('编辑表单数据:', form)
  dialogVisible.value = true
}

// 获取食谱名称的辅助函数
const getRecipeName = (recipeId, recipeNameFromRow, storedRecipeName) => {
  // 如果有直接的名称，优先使用
  if (recipeNameFromRow || storedRecipeName) {
    return recipeNameFromRow || storedRecipeName
  }
  
  // 如果只有recipeId，从recipeList中查找
  if (recipeId && recipeList.value.length > 0) {
    const foundRecipe = recipeList.value.find(r => r.id === recipeId)
    if (foundRecipe) {
      return foundRecipe.name
    }
  }
  
  // 默认情况
  return '未命名食谱'
}

// 删除计划
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该膳食计划吗？', '提示', {
      type: 'warning'
    })
    
    // 保存当前状态
    const currentViewMode = viewMode.value
    const isShowingDayDetail = dayPlanDialogVisible.value
    const currentDetailDate = selectedDay.value?.date
    
    await request.delete(`/mealPlan/delete/${id}`)
    ElMessage.success('删除成功')
    
    // 重新加载数据
    await loadData()
    
    // 如果正在显示日期详情，刷新详情
    if (isShowingDayDetail && currentDetailDate) {
      console.log('删除后刷新日期详情')
      await showDayPlanDetail(currentDetailDate)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    const user = JSON.parse(localStorage.getItem('system-user') || '{}')
    form.userId = user.id
    
    // 修复：统一日期格式，避免时区偏移
    // 将 Date 对象转换为本地日期字符串 (YYYY-MM-DD)
    let planDateForSave = form.planDate
    if (form.planDate && form.planDate instanceof Date) {
      const year = form.planDate.getFullYear()
      const month = (form.planDate.getMonth() + 1).toString().padStart(2, '0')
      const day = form.planDate.getDate().toString().padStart(2, '0')
      planDateForSave = `${year}-${month}-${day}`
    }
    
    // 保存提交前的日期，用于后续更新
    const submittedDate = planDateForSave
    
    // 根据recipeId获取食谱名称
    let recipeName = ''
    if (form.recipeId) {
      const selectedRecipe = recipeList.value.find(r => r.id === form.recipeId)
      if (selectedRecipe) {
        recipeName = selectedRecipe.name
      }
    }

    // 准备提交数据（排除不必要字段）
    const submitData = {
      id: form.id,
      userId: user.id,
      planName: form.planName,
      planDate: planDateForSave,
      mealType: form.mealType,
      recipeId: form.recipeId || null,
      recipeName: recipeName, // 保存食谱名称
      customMeal: form.customMeal || '',
      calories: form.calories || null,
      notes: form.notes || ''
    }
    
    console.log('提交表单数据:', submitData)
    console.log('当前视图模式:', viewMode.value)
    console.log('提交的日期:', submittedDate)
    
    if (form.id) {
      await request.put('/mealPlan/update', submitData)
      ElMessage.success('修改成功')
    } else {
      await request.post('/mealPlan/add', submitData)
      ElMessage.success('新增成功')
    }
    
    // 先关闭对话框
    dialogVisible.value = false
    
    // 保存当前的视图模式和日期详情状态
    const currentViewMode = viewMode.value
    const isShowingDayDetail = dayPlanDialogVisible.value
    const currentDetailDate = selectedDay.value?.date
    
    console.log('保存当前视图模式:', currentViewMode)
    console.log('是否显示日期详情:', isShowingDayDetail)
    console.log('当前详情日期:', currentDetailDate)
    
    // 根据当前视图模式设置不同的加载策略
    if (currentViewMode === 'calendar') {
      // 日历视图：重置到第一页并重新加载数据
      pageNum.value = 1
      await loadData()
      // 通过创建新的Date对象来触发响应式更新
      currentDate.value = new Date(currentDate.value.getTime())
      console.log('日历视图强制刷新完成')
    } else {
      // 列表视图：重置到第一页并重新加载数据
      pageNum.value = 1
      await loadData()
      console.log('列表视图数据重新加载完成')
    }
    
    // 如果正在显示日期详情，且提交的是相同日期，则刷新详情
    if (isShowingDayDetail && currentDetailDate === submittedDate) {
      console.log('刷新日期详情对话框')
      await showDayPlanDetail(currentDetailDate)
    }
    
    // 验证视图模式没有被意外修改
    console.log('操作后视图模式:', viewMode.value)
    if (viewMode.value !== currentViewMode) {
      console.warn('⚠️ 警告：视图模式被意外修改！从', currentViewMode, '变为了', viewMode.value)
      // 恢复正确的视图模式 - 使用nextTick确保在下一个更新周期恢复
      await nextTick()
      viewMode.value = currentViewMode
      console.log('✅ 已恢复正确的视图模式:', currentViewMode)
    }
    
    // 清空表单
    if (formRef.value) {
      formRef.value.resetFields()
    }
  } catch (error) {
    console.error('表单提交失败:', error)
    ElMessage.error('操作失败，请重试')
  }
}

// 对话框关闭
const handleDialogClose = () => {
  console.log('对话框关闭事件触发，当前视图模式:', viewMode.value)
  if (formRef.value) {
    formRef.value.resetFields()
  }
  // 防止任何意外的视图模式修改
  console.log('对话框关闭完成，视图模式保持为:', viewMode.value)
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadData()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadData()
}

// 月份切换
const prevMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1, 1)
  updateCurrentMonth()
}

const nextMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 1)
  updateCurrentMonth()
}

// 查看日计划
const viewDayPlan = async (day) => {
  if (!day.isCurrentMonth) return
  
  selectedDay.value = day
  try {
    const user = JSON.parse(localStorage.getItem('system-user') || '{}')
    
    // 确保用户ID存在
    if (!user.id) {
      ElMessage.error('请先登录')
      return
    }
    
    // 修复：确保日期处理一致，避免时区问题
    // 直接使用日历生成的本地日期字符串，避免任何时区转换
    const planDate = day.date
    console.log('查看日计划，原日期:', day.date, '处理后的日期:', planDate)
    
    // 统一的日期比较函数
    const normalizeDateStr = (dateString) => {
      if (!dateString) return ''
      let normalized = dateString
      if (normalized.includes('T')) {
        normalized = normalized.split('T')[0]
      }
      return normalized
    }
    
    // 方法1：从本地已有数据中查找（优先使用，避免额外API调用）
    const localData = tableData.value.filter(item => {
      return normalizeDateStr(item.planDate) === planDate
    })
    
    console.log('从本地数据中找到的日计划:', localData)
    
    // 如果本地数据为空，再调用API获取
    let finalData = localData
    if (localData.length === 0) {
      console.log('本地数据为空，调用API获取日计划...')
      
      // 使用与后端完全匹配的API参数
      const params = {
        userId: user.id,
        planDate: planDate
      }
      
      console.log('日计划API参数:', params)
      
      const res = await request.get('/mealPlan/selectByUser', { params })
      console.log('日计划API响应:', res)
      
      // 统一处理API响应数据结构
      let actualData = []
      if (res && res.data) {
        // 分页响应结构：{ list: [], total: 100 }
        if (res.data.list && Array.isArray(res.data.list)) {
          actualData = res.data.list
          console.log('日计划：分页数据结构')
        } 
        // 非分页响应结构：直接返回数组
        else if (Array.isArray(res.data)) {
          actualData = res.data
          console.log('日计划：非分页数据结构')
        }
        // 其他可能的响应结构
        else if (res.data.data && Array.isArray(res.data.data)) {
          actualData = res.data.data
          console.log('日计划：嵌套数据结构')
        }
      }
      
      console.log('API获取到的日计划数据:', actualData)
      
      // 过滤日期匹配的数据
      finalData = actualData.filter(item => {
        return normalizeDateStr(item.planDate) === planDate
      })
    }
    
    console.log('最终日计划数据:', finalData)
    dayPlanData.value = finalData
    dayPlanDialogVisible.value = true
    
    // 同时更新本地数据，确保数据同步
    if (localData.length === 0 && finalData.length > 0) {
      console.log('更新本地数据以确保同步')
      // 避免重复添加数据
      const existingIds = new Set(tableData.value.map(item => item.id))
      const newData = finalData.filter(item => !existingIds.has(item.id))
      if (newData.length > 0) {
        tableData.value = [...tableData.value, ...newData]
      }
    }
  } catch (error) {
    ElMessage.error('加载日计划失败')
    console.error('查看日计划错误:', error)
    
    // 详细的错误信息
    if (error.response) {
      console.error('错误响应状态:', error.response.status)
      console.error('错误响应数据:', error.response.data)
    }
  }
}

// 工具函数 - 统一使用本地时间避免时区问题
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  
  let date
  // 处理不同格式的日期字符串
  if (dateStr.includes('T')) {
    // ISO格式: 2025-11-15T00:00:00
    date = new Date(dateStr)
  } else if (dateStr.includes('-')) {
    // 日期格式: 2025-11-15
    const [year, month, day] = dateStr.split('-').map(Number)
    date = new Date(year, month - 1, day) // 注意月份要减1
  } else {
    date = new Date(dateStr)
  }
  
  // 使用本地时间格式化
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const formatDetailDate = (dateStr) => {
  if (!dateStr) return ''
  
  let date
  // 处理不同格式的日期字符串
  if (dateStr.includes('T')) {
    // ISO格式: 2025-11-15T00:00:00
    date = new Date(dateStr)
  } else if (dateStr.includes('-')) {
    // 日期格式: 2025-11-15
    const [year, month, day] = dateStr.split('-').map(Number)
    date = new Date(year, month - 1, day) // 注意月份要减1
  } else {
    date = new Date(dateStr)
  }
  
  // 使用本地时间格式化
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${year}年${month}月${day}日`
}

const getMealTypeTag = (mealType) => {
  const tags = {
    '早餐': 'success',
    '午餐': 'warning',
    '晚餐': 'danger',
    '加餐': 'info'
  }
  return tags[mealType] || 'info'
}

const getMealTypeIcon = (mealType) => {
  const icons = {
    '早餐': '🌅',
    '午餐': '☀️',
    '晚餐': '🌙',
    '加餐': '🍎'
  }
  return icons[mealType] || '🍽️'
}

// 为指定日期添加计划
const handleAddForDate = (date) => {
  console.log('为指定日期添加计划:', date)
  
  // 清空表单
  Object.keys(form).forEach(key => {
    form[key] = ''
  })
  
  form.id = null
  // 确保日期格式正确，转换为Date对象供日期选择器使用
  form.planDate = new Date(date)
  searchDate.value = new Date(date)
  
  console.log('设置表单日期:', form.planDate)
  dialogVisible.value = true
}

// 关闭日计划对话框
const closeDayPlanDialog = () => {
  dayPlanDialogVisible.value = false
  selectedDay.value = null
  dayPlanData.value = []
}

// 计算属性
const dialogTitle = computed(() => {
  return form.id ? '编辑膳食计划' : '新增膳食计划'
})
</script>

<style scoped>
.calendar-container {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background-color: #f5f7fa;
  border-bottom: 1px solid #e0e0e0;
}

.calendar-header-cell {
  padding: 12px;
  text-align: center;
  font-weight: bold;
  color: #606266;
}

.calendar-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: repeat(6, 1fr);
}

.calendar-day {
  min-height: 100px;
  border: 1px solid #f0f0f0;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.calendar-day:hover {
  background-color: #f5f7fa;
}

.calendar-day.current-month {
  background-color: white;
}

.calendar-day:not(.current-month) {
  background-color: #fafafa;
  color: #c0c4cc;
}

.calendar-day.today {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.calendar-day.has-plan {
  background-color: #f0f9ff;
}

.day-number {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.meal-indicators {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.meal-indicator {
  font-size: 12px;
}
</style>