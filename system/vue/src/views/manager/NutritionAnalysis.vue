<template>
  <div class="recommendation-container">
    <h1>营养需求分析</h1>
    
    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" type="border-card" @tab-click="handleTabClick">
      <el-tab-pane label="健康数据" name="healthData">
        <el-row :gutter="20">
          <!-- 健康数据输入表单 -->
          <el-col :span="16">
            <el-form 
              ref="healthDataFormRef" 
              :model="healthDataForm" 
              :rules="healthDataRules" 
              label-width="100px" 
              class="health-data-form"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="身高(cm)" prop="height">
                    <el-input v-model="healthDataForm.height" type="number" placeholder="请输入身高" />
                  </el-form-item>
                  <el-form-item label="体重(kg)" prop="weight">
                    <el-input v-model="healthDataForm.weight" type="number" placeholder="请输入体重" />
                  </el-form-item>
                  <el-form-item label="年龄" prop="age">
                    <el-input v-model="healthDataForm.age" type="number" placeholder="请输入年龄" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="性别" prop="gender">
                    <el-radio-group v-model="healthDataForm.gender">
                      <el-radio label="男">男</el-radio>
                      <el-radio label="女">女</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="活动水平" prop="activityLevel">
                    <el-select v-model="healthDataForm.activityLevel" placeholder="请选择活动水平">
                      <el-option label="久坐" value="久坐" />
                      <el-option label="轻度" value="轻度" />
                      <el-option label="中度" value="中度" />
                      <el-option label="重度" value="重度" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="健康目标" prop="goal">
                    <el-select v-model="healthDataForm.goal" placeholder="请选择健康目标">
                      <el-option label="维持健康" value="维持健康" />
                      <el-option label="减重" value="减重" />
                      <el-option label="增肌" value="增肌" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-form-item>
                <el-button type="primary" @click="calculateNutritionPlan">计算营养需求</el-button>
                <el-button @click="saveHealthData">保存健康数据</el-button>
              </el-form-item>
            </el-form>
          </el-col>
          
          <!-- 右侧BMI显示区域 -->
          <el-col :span="8">
            <div class="bmi-display-card">
              <h3>BMI 计算结果</h3>
              <div v-if="showBMI && healthDataForm.height && healthDataForm.weight" class="bmi-result">
                  <div class="bmi-value">{{ calculateBMI().toFixed(1) }}</div>
                  <div class="bmi-category">{{ getBMICategory(calculateBMI()) }}</div>
                </div>
                <div v-else class="bmi-empty">
                <p>请输入身高和体重以计算BMI</p>
              </div>
              <div class="bmi-hint">
                <p>点击"保存健康数据"后将显示您的BMI信息</p>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
      
      <el-tab-pane label="营养分析" name="nutritionAnalysis">
        <!-- 营养需求展示 -->
        <div v-if="nutritionPlan" class="nutrition-analysis">
          <h3>您的营养需求分析</h3>
          <div class="nutrition-stats">
            <div class="nutrition-stat-item">
              <div class="nutrition-stat-label">基础代谢率(BMR)</div>
              <div class="nutrition-stat-value">{{ nutritionPlan.bmr }} 卡路里</div>
            </div>
            <div class="nutrition-stat-item">
              <div class="nutrition-stat-label">总能量消耗(TDEE)</div>
              <div class="nutrition-stat-value">{{ nutritionPlan.tdee }} 卡路里</div>
            </div>
            <div class="nutrition-stat-item">
            <div class="nutrition-stat-label">推荐卡路里</div>
            <div class="nutrition-stat-value">{{ nutritionPlan.recommendedCalories }} 卡路里</div>
          </div>
          </div>
          
          <div class="nutrition-details">
            <h4>每日营养素需求</h4>
            <el-table :data="getNutrientData()" style="width: 100%">
              <el-table-column prop="name" label="营养素" />
              <el-table-column prop="value" label="需求" />
              <el-table-column prop="unit" label="单位" />
            </el-table>
          </div>
          
          <!-- 营养摄入建议 -->
          <div class="nutrition-advice">
            <h4>营养建议</h4>
            <p>{{ getNutritionAdvice() }}</p>
          </div>
        </div>
        <div v-else class="empty-state">
          <el-empty description="请先填写健康数据并计算营养需求" />
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 加载中提示 -->
    <el-loading v-loading="loading" lock text="处理中..." />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

export default {
  name: 'NutritionAnalysis',
  setup() {
    // 响应式数据
    const activeTab = ref('healthData')
    const loading = ref(false)
    const healthDataFormRef = ref()
    const showBMI = ref(false) // 控制BMI结果是否显示，初始为false
    
    // 健康数据表单
    const healthDataForm = reactive({
      height: null,
      weight: null,
      age: null,
      gender: '男',
      activityLevel: '中度',
      goal: '维持健康'
    })
    
    // 表单验证规则
    const healthDataRules = {
      height: [
        { required: true, message: '请输入身高', trigger: 'blur' },
        { 
          type: 'number', 
          message: '身高应在100-250cm之间', 
          trigger: 'change',
          validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') {
              callback()
            } else if (value < 100 || value > 250) {
              callback(new Error('身高应在100-250cm之间'))
            } else {
              callback()
            }
          }
        }
      ],
      weight: [
        { required: true, message: '请输入体重', trigger: 'blur' },
        { 
          type: 'number', 
          message: '体重应在30-300kg之间', 
          trigger: 'change',
          validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') {
              callback()
            } else if (value < 30 || value > 300) {
              callback(new Error('体重应在30-300kg之间'))
            } else {
              callback()
            }
          }
        }
      ],
      age: [
        { required: true, message: '请输入年龄', trigger: 'blur' },
        { 
          type: 'number', 
          message: '年龄应在1-150之间', 
          trigger: 'change',
          validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') {
              callback()
            } else if (value < 1 || value > 150) {
              callback(new Error('年龄应在1-150之间'))
            } else {
              callback()
            }
          }
        }
      ],
      gender: [
        { required: false, message: '请选择性别', trigger: 'change' }
      ],
      activityLevel: [
        { required: false, message: '请选择活动水平', trigger: 'change' }
      ],
      goal: [
        { required: false, message: '请选择健康目标', trigger: 'change' }
      ]
    }
    
    // 营养计划数据
    const nutritionPlan = ref(null)
    
    // 移除了食谱推荐和膳食计划相关的数据
    
    // 处理标签页切换
    const handleTabClick = (tab) => {
      activeTab.value = tab.paneName
      console.log('标签页切换到:', activeTab.value)
      
      // 如果切换到营养分析标签页，确保有营养计划
      if (activeTab.value === 'nutritionAnalysis') {
        console.log('切换到营养分析标签页，检查是否有营养计划')
        if (!nutritionPlan.value) {
          console.log('没有营养计划，尝试计算')
          calculateNutritionPlan()
        }
      }
    }
    
    // 计算营养计划
    const calculateNutritionPlan = async () => {
      try {
        await healthDataFormRef.value.validate()
        loading.value = true
        
        const response = await request.post('/nutritionAnalysis/nutritionPlan', healthDataForm)
        
        if (response && response.code === '200') {
          nutritionPlan.value = response.data
          // 计算BMI并添加到营养计划中
          const heightInMeters = healthDataForm.height / 100 // 将身高从厘米转换为米
          const bmi = healthDataForm.weight / (heightInMeters * heightInMeters)
          nutritionPlan.value.bmi = bmi
          
          ElMessage.success('营养需求计算成功')
          activeTab.value = 'nutritionAnalysis'
        } else {
          // 清除之前可能存在的错误状态
          ElMessage.error(response?.msg || '计算失败，请稍后重试')
        }
      } catch (error) {
        console.error('计算营养计划失败:', error)
        // 避免显示重复的错误信息
        if (!error.message || !error.message.includes('Validation failed')) {
          ElMessage.error('计算失败，请稍后重试')
        }
      } finally {
        loading.value = false
      }
    }
    
    // 计算BMI
    const calculateBMI = () => {
      if (!healthDataForm.height || !healthDataForm.weight) return 0
      const heightInMeters = healthDataForm.height / 100
      return healthDataForm.weight / (heightInMeters * heightInMeters)
    }
    
    // 获取BMI分类
    const getBMICategory = (bmi) => {
      if (bmi < 18.5) return '偏瘦'
      if (bmi < 24) return '正常'
      if (bmi < 28) return '超重'
      if (bmi < 35) return '肥胖'
      return '重度肥胖'
    }
    
    // 保存健康数据
    const saveHealthData = async () => {
      try {
        await healthDataFormRef.value.validate()
        loading.value = true
        
        // 获取当前用户ID
        const user = JSON.parse(localStorage.getItem('system-user') || '{}')
        if (!user.id) {
          ElMessage.error('用户未登录，请重新登录')
          loading.value = false
          return
        }
        
        // 确保核心健康数据是数字类型
        const healthDataToSave = {
          ...healthDataForm,
          height: Number(healthDataForm.height),
          weight: Number(healthDataForm.weight),
          age: Number(healthDataForm.age)
        }
        
        // 确保数据类型正确
        console.log('保存的健康数据类型:', {
          height: typeof healthDataToSave.height,
          weight: typeof healthDataToSave.weight,
          age: typeof healthDataToSave.age,
          heightValue: healthDataToSave.height,
          weightValue: healthDataToSave.weight,
          ageValue: healthDataToSave.age
        })
        
        const response = await request.put(`/nutritionAnalysis/updateHealthData/${user.id}`, healthDataToSave)
        
        if (response && response.code === '200') {
          // 计算并显示BMI信息
          if (healthDataForm.height && healthDataForm.weight) {
            const bmi = calculateBMI()
            const category = getBMICategory(bmi)
            showBMI.value = true // 设置为true，显示BMI结果
            ElMessage.success(`健康数据保存成功！您的BMI为${bmi.toFixed(1)} (${category})`)
          } else {
            ElMessage.success('健康数据保存成功')
          }
          
          // 立即更新本地数据类型，避免验证失败
          healthDataForm.height = Number(healthDataForm.height)
          healthDataForm.weight = Number(healthDataForm.weight)
          healthDataForm.age = Number(healthDataForm.age)
          
          // 重新计算营养计划
          try {
            const nutritionResponse = await request.post('/nutritionAnalysis/nutritionPlan', healthDataToSave)
            if (nutritionResponse.code === '200') {
              nutritionPlan.value = nutritionResponse.data
              console.log('营养计划重新计算成功')
              
              // 强制更新健康数据状态
              const updatedHealthData = await request.get(`/user/selectById/${user.id}`)
              if (updatedHealthData.code === '200' && updatedHealthData.data) {
                console.log('更新后的健康数据:', updatedHealthData.data)
                // 确保获取的数据类型正确
                if (updatedHealthData.data.height) healthDataForm.height = Number(updatedHealthData.data.height)
                if (updatedHealthData.data.weight) healthDataForm.weight = Number(updatedHealthData.data.weight)
                if (updatedHealthData.data.age) healthDataForm.age = Number(updatedHealthData.data.age)
                if (updatedHealthData.data.gender) healthDataForm.gender = updatedHealthData.data.gender
                if (updatedHealthData.data.activityLevel) healthDataForm.activityLevel = updatedHealthData.data.activityLevel
                if (updatedHealthData.data.goal) healthDataForm.goal = updatedHealthData.data.goal
              }
              
              // 如果当前在食谱推荐标签页，自动显示推荐内容
              if (activeTab.value === 'recipeRecommendation') {
                await getRecipeRecommendations()
              } else {
                // 即使不在推荐标签页，也设置showRecommendations为true，以便切换到该标签页时能立即显示
                showRecommendations.value = true
              }
            }
          } catch (nutritionError) {
            console.error('重新计算营养计划失败:', nutritionError)
          }
        } else {
          ElMessage.error(response?.msg || '保存失败，请稍后重试')
        }
      } catch (error) {
        console.error('保存健康数据失败:', error)
        // 避免显示重复的错误信息
        if (!error.message || !error.message.includes('Validation failed')) {
          ElMessage.error('保存失败，请稍后重试')
        }
      } finally {
        loading.value = false
      }
    }
    
    // 加载用户健康数据
    const loadUserHealthData = async (forceUpdate = false) => {
      try {
        loading.value = true
        
        // 获取当前用户ID
        const user = JSON.parse(localStorage.getItem('system-user') || '{}')
        if (!user.id) {
          console.error('用户未登录')
          loading.value = false
          return
        }
        
        const response = await request.get(`/user/selectById/${user.id}`)
        
        if (response.code === '200' && response.data) {
          // 将用户数据填充到健康数据表单
          const userData = response.data
          // 不要使用空字符串作为默认值，保持原始的null值或数字
          
          // 只有在forceUpdate为true或本地没有有效数据时才更新核心健康数据
          if (forceUpdate || (healthDataForm.height === null && userData.height)) {
            healthDataForm.height = userData.height
          }
          if (forceUpdate || (healthDataForm.weight === null && userData.weight)) {
            healthDataForm.weight = userData.weight
          }
          if (forceUpdate || (healthDataForm.age === null && userData.age)) {
            healthDataForm.age = userData.age
          }
          
          // 对于非核心数据，可以考虑更宽松的更新策略
          if (userData.gender) healthDataForm.gender = userData.gender
          if (userData.activityLevel) healthDataForm.activityLevel = userData.activityLevel
          if (userData.goal) healthDataForm.goal = userData.goal
          
          // 检查是否有核心健康数据（身高、体重、年龄），确保是有效的数字
            if (typeof healthDataForm.height === 'number' && healthDataForm.height > 0 &&
                typeof healthDataForm.weight === 'number' && healthDataForm.weight > 0 &&
                typeof healthDataForm.age === 'number' && healthDataForm.age > 0) {
            // 自动计算营养计划
            try {
              const nutritionResponse = await request.post('/nutritionAnalysis/nutritionPlan', healthDataForm)
              if (nutritionResponse.code === '200') {
                nutritionPlan.value = nutritionResponse.data
                console.log('营养计划计算成功:', nutritionPlan.value)
              }
            } catch (nutritionError) {
              console.error('自动计算营养计划失败:', nutritionError)
            }
          }
        }
      } catch (error) {
        console.error('加载健康数据失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取营养素数据用于表格显示
    const getNutrientData = () => {
      if (!nutritionPlan.value || !nutritionPlan.value.nutrients) {
        return []
      }
      
      const { nutrients } = nutritionPlan.value
      return [
        { name: '蛋白质', value: nutrients.protein, unit: 'g' },
        { name: '碳水化合物', value: nutrients.carbohydrate, unit: 'g' },
        { name: '脂肪', value: nutrients.fat, unit: 'g' }
      ]
    }
    
    // 获取营养建议
    const getNutritionAdvice = () => {
      if (!nutritionPlan.value) return ''
      
      const { goal } = healthDataForm
      switch (goal) {
        case '减重':
          return '减重期间建议适当增加蛋白质摄入，控制碳水化合物摄入，选择低脂食物，保证足够的膳食纤维和水分摄入。'
        case '增肌':
          return '增肌期间建议增加优质蛋白质摄入，适量增加碳水化合物，保证足够的热量盈余，结合适当的力量训练。'
        case '维持健康':
        default:
          return '维持健康期间建议保持均衡的饮食结构，摄入足够的各类营养素，合理分配三餐比例。'
      }
    }
    
    // 移除了食谱推荐和膳食计划相关的函数
    
    // 检查用户健康数据完整性
    const checkHealthDataComplete = async () => {
      console.log('开始检查健康数据完整性')
      
      // 获取当前用户ID
      const user = JSON.parse(localStorage.getItem('system-user') || '{}')
      if (!user.id) {
        ElMessage.error('用户未登录，请重新登录')
        return false
      }
      
      // 如果没有核心健康数据，先尝试从服务器加载
      if (!healthDataForm.height || !healthDataForm.weight || !healthDataForm.age) {
        await loadUserHealthData(false)
      }
      
      // 检查并转换字段为数字类型
      let height = Number(healthDataForm.height || 0)
      let weight = Number(healthDataForm.weight || 0)
      let age = Number(healthDataForm.age || 0)
      
      // 检查必填字段是否存在且为有效的正数
      const isHeightValid = !isNaN(height) && height > 0 && height < 300
      const isWeightValid = !isNaN(weight) && weight > 0 && weight < 500
      const isAgeValid = !isNaN(age) && age > 0 && age < 150
      const isGenderValid = healthDataForm.gender
      const isActivityLevelValid = healthDataForm.activityLevel
      const isGoalValid = healthDataForm.goal
      
      // 输出详细的检查信息
      console.log('健康数据检查详情:', {
        healthDataForm: healthDataForm,
        height: { value: healthDataForm.height, type: typeof healthDataForm.height, converted: height, valid: isHeightValid },
        weight: { value: healthDataForm.weight, type: typeof healthDataForm.weight, converted: weight, valid: isWeightValid },
        age: { value: healthDataForm.age, type: typeof healthDataForm.age, converted: age, valid: isAgeValid },
        gender: { value: healthDataForm.gender, valid: isGenderValid },
        activityLevel: { value: healthDataForm.activityLevel, valid: isActivityLevelValid },
        goal: { value: healthDataForm.goal, valid: isGoalValid }
      })
      
      // 检查核心数据有效性
      if (!isHeightValid || !isWeightValid || !isAgeValid) {
        let missingFields = []
        if (!isHeightValid) missingFields.push('身高')
        if (!isWeightValid) missingFields.push('体重')
        if (!isAgeValid) missingFields.push('年龄')
        
        console.log('缺失或无效的核心字段:', missingFields)
        return false
      }
      
      // 更新健康数据表单中的值为数字类型
      healthDataForm.height = height
      healthDataForm.weight = weight
      healthDataForm.age = age
      
      // 检查本地是否有营养计划数据
      if (!nutritionPlan.value) {
        // 如果有健康数据但没有营养计划，自动计算
        try {
          loading.value = true
          const nutritionResponse = await request.post('/recommendation/nutritionPlan', healthDataForm)
          if (nutritionResponse.code === '200') {
            nutritionPlan.value = nutritionResponse.data
            return true
          } else {
            console.warn('营养计划计算失败:', nutritionResponse.msg)
            return false
          }
        } catch (error) {
          console.error('自动计算营养计划失败:', error)
          return false
        } finally {
          loading.value = false
        }
      }
      
      return true
    }

    // 移除了食谱推荐和膳食计划相关的函数
    
    // handleTabClick函数已在上方声明，此处不再重复定义

    // 页面加载时执行
    onMounted(() => {
      // 加载用户健康数据
      loadUserHealthData()
    })
    
    return {
        activeTab,
        loading,
        healthDataFormRef,
        healthDataForm,
        healthDataRules,
        showBMI,
        nutritionPlan,
        handleTabClick,
        calculateNutritionPlan,
        saveHealthData,
        getNutrientData,
        getNutritionAdvice,
        getBMICategory,
        calculateBMI
      }
  }
}
</script>

<style lang="scss" scoped>
.recommendation-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
  
  h1 {
    margin-bottom: 30px;
    color: #303133;
  }
  
  .el-tabs {
    background-color: #fff;
    border-radius: 8px;
    overflow: hidden;
  }
}

/* BMI显示卡片样式 */
.bmi-display-card {
  background-color: #f0f2f5;
  border-radius: 8px;
  padding: 20px;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  
  h3 {
    margin-bottom: 30px;
    color: #303133;
    font-weight: 600;
  }
  
  .bmi-result {
    text-align: center;
    margin-bottom: 20px;
  }
  
  .bmi-value {
    font-size: 48px;
    font-weight: bold;
    color: #409eff;
    margin-bottom: 10px;
  }
  
  .bmi-category {
    font-size: 24px;
    color: #606266;
    font-weight: 500;
  }
  
  .bmi-empty {
    text-align: center;
    color: #909399;
    font-size: 16px;
  }
  
  .bmi-hint {
    text-align: center;
    color: #909399;
    font-size: 14px;
    margin-top: 30px;
    font-style: italic;
  }
}

/* 健康数据表单样式 */
.health-data-form {
  padding: 20px;
  background-color: #fff;
  
  .el-form-item {
    margin-bottom: 20px;
  }
  
  .el-row {
    margin-bottom: 20px;
  }
  
  /* 重置输入框默认样式 */
  .el-input__wrapper {
    box-shadow: none !important;
    border-color: #dcdfe6;
    
    &:focus-within {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) !important;
    }
  }
  
  /* 只在真正错误状态下显示红色边框 */
  .el-form-item.is-error {
    .el-input__wrapper {
      border-color: #f56c6c;
      box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2) !important;
    }
    
    .el-form-item__error {
      color: #f56c6c;
      display: block !important;
    }
  }
  
  /* 确保非错误状态下不显示错误提示 */
  .el-form-item:not(.is-error) {
    .el-form-item__error {
      display: none !important;
    }
  }
  
  /* 有效输入时的样式 */
  .el-input__wrapper.is-valid {
    border-color: #67c23a;
    box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2) !important;
  }
}

/* 营养分析样式 */
.nutrition-analysis {
  padding: 20px;
  
  h3 {
    margin-bottom: 20px;
    color: #303133;
  }
  
  h4 {
    margin: 20px 0 15px 0;
    color: #606266;
  }
  
  .nutrition-stats {
    display: flex;
    gap: 20px;
    margin-bottom: 30px;
  }
  
  .nutrition-stat-item {
    flex: 1;
    background-color: #f0f9eb;
    padding: 20px;
    border-radius: 8px;
    text-align: center;
    
    .nutrition-stat-label {
      font-size: 14px;
      color: #606266;
      margin-bottom: 10px;
    }
    
    .nutrition-stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #67c23a;
  }
  
  .bmi-category {
    font-size: 16px;
    font-weight: normal;
    color: #606266;
  }
  
  .bmi-category {
    font-size: 16px;
    font-weight: normal;
    color: #606266;
  }
  }
  
  .nutrition-advice {
    background-color: #ecf5ff;
    padding: 15px;
    border-radius: 8px;
    border-left: 4px solid #409eff;
    
    p {
      margin: 0;
      line-height: 1.6;
    }
  }
}

/* 食谱推荐样式 */
.recipe-recommendation {
  padding: 20px;
  
  .meal-type-selector {
    margin-bottom: 30px;
    
    .recommendation-controls {
      display: flex;
      gap: 10px;
      margin-top: 15px;
      
      .el-select {
        width: 120px;
      }
    }
  }
  
  .recipe-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
  }
  
  .recipe-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    
    .recipe-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .recipe-card-body {
      display: flex;
      flex-direction: column;
      gap: 10px;
      
      .recipe-nutrition {
        display: flex;
        gap: 15px;
        font-size: 14px;
        color: #606266;
        margin-bottom: 10px;
      }
      
      .recipe-description {
        color: #909399;
        font-size: 14px;
        margin-bottom: 15px;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }
  }
}

/* 膳食计划样式 */
.meal-plan {
  padding: 20px;
  
  .meal-type-section {
    margin-bottom: 30px;
    
    h4 {
      background-color: #f0f9eb;
      padding: 10px 15px;
      border-radius: 4px;
      color: #67c23a;
      margin-bottom: 15px;
    }
    
    .meal-recipes {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 15px;
    }
    
    .meal-recipe-card {
      .meal-recipe-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
      }
      
      .meal-recipe-nutrition {
        display: flex;
        gap: 15px;
        font-size: 14px;
        color: #606266;
      }
    }
  }
  
  .meal-plan-actions {
    margin-top: 30px;
    display: flex;
    gap: 10px;
  }
}

/* 空状态样式 */
.empty-state {
  padding: 60px 0;
  text-align: center;
}
</style>