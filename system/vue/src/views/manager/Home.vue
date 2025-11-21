<template>
  <div class="home-container">
    <!-- 欢迎卡片 -->
    <div class="card" style="line-height: 30px; margin-bottom: 20px;">
      <div>欢迎{{ data.user.name }}呀，今天你做好膳食计划了吗~~~</div>
    </div>

    <!-- 一周卡路里趋势图 -->
    <div class="chart-card">
      <div class="chart-title">本周每日膳食计划卡路里</div>
      <div id="calorieTrendChart" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, onUnmounted } from "vue";
import * as echarts from 'echarts';
import request from "@/utils/request";
import { ElMessage } from 'element-plus';

// 生成本周的日期数组（格式：MM-DD）
const generateCurrentWeekDates = () => {
  const dates = [];
  const today = new Date();
  // 获取本周第一天（周日为0，周一为1，所以需要调整）
  const dayOfWeek = today.getDay();
  const diff = today.getDate() - dayOfWeek;
  
  // 生成从周日到周六的日期
  for (let i = 0; i < 7; i++) {
    const date = new Date(today);
    date.setDate(diff + i);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    dates.push(`${month}-${day}`);
  }
  return dates;
};

const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  charts: {
    calorieTrend: null
  },
  // 一周卡路里数据 - 初始为空，将从API实时获取
  weeklyCalorieData: {
    dates: generateCurrentWeekDates(),
    calorieIntake: []
  }
});

// 计算每日卡路里合计（处理可能的复杂数据结构）
const calculateDailyCalorieTotal = (dayData) => {
  // 如果已经是数字，直接返回
  if (typeof dayData === 'number') {
    return dayData;
  }
  
  // 如果是数组，求和（可能是一天中的多个餐次）
  if (Array.isArray(dayData)) {
    return dayData.reduce((sum, item) => {
      // 处理数组元素可能是对象的情况
      if (typeof item === 'object' && item !== null) {
        return sum + (item.calories || item.value || 0);
      }
      return sum + (Number(item) || 0);
    }, 0);
  }
  
  // 如果是对象，尝试获取其中的卡路里值
  if (typeof dayData === 'object' && dayData !== null) {
    return dayData.calories || dayData.value || dayData.total || 0;
  }
  
  // 默认返回0
  return 0;
};

// 生成临时模拟数据的辅助函数
const generateMockCalorieData = (dates) => {
  // 生成合理的模拟卡路里数据，范围在1800-2200之间
  const mockData = [];
  for (let i = 0; i < dates.length; i++) {
    // 为了看起来更真实，生成一个有波动的随机数
    const baseCalories = 2000;
    const variation = Math.floor(Math.random() * 400) - 200; // -200到199的波动
    mockData.push(baseCalories + variation);
  }
  return mockData;
};

// 从膳食计划获取卡路里数据
const fetchCalorieDataFromMealPlan = async () => {
  try {
    console.log('正在从膳食计划API实时获取每日卡路里数据...');
    const dates = generateCurrentWeekDates();
    const calorieIntake = [];
    const user = JSON.parse(localStorage.getItem('system-user') || '{}');
    
    // 确保用户ID存在
    if (!user.id) {
      console.error('用户ID不存在，请先登录');
      ElMessage.error('用户ID不存在，请先登录');
      
      // 用户未登录，显示0值数组
      const zeroData = dates.map(() => 0);
      console.log('用户未登录，显示0值数据:', zeroData);
      
      data.weeklyCalorieData = {
        dates,
        calorieIntake: zeroData
      };
      return;
    }
    
    // 一次性获取一周的膳食计划数据，而不是每天单独请求
    const params = {
      userId: user.id,
      // 可以添加日期范围查询，如果后端支持的话
    };
    
    console.log('膳食计划API参数:', params);
    
    const response = await request.get('/mealPlan/selectByUser', { params });
    console.log('膳食计划API完整响应:', response);
    
    // 统一处理API响应数据结构
    let mealPlanData = [];
    if (response && response.data) {
      // 分页响应结构：{ list: [], total: 100 }
      if (response.data.list && Array.isArray(response.data.list)) {
        mealPlanData = response.data.list;
        console.log('分页数据结构：使用list字段，数据长度:', mealPlanData.length);
      }
      // 非分页响应结构：直接返回数组
      else if (Array.isArray(response.data)) {
        mealPlanData = response.data;
        console.log('非分页数据结构：直接返回数组，数据长度:', mealPlanData.length);
      }
      // 其他可能的响应结构
      else if (response.data.data && Array.isArray(response.data.data)) {
        mealPlanData = response.data.data;
        console.log('嵌套数据结构：使用data字段，数据长度:', mealPlanData.length);
      }
    }
    
    console.log('解析后的膳食计划数据:', mealPlanData);
    
    // 为每天计算卡路里总和
    for (let i = 0; i < dates.length; i++) {
      const date = dates[i];
      
      // 过滤出当天的膳食计划
      const dayPlans = mealPlanData.filter(plan => {
        let planDateStr = plan.planDate;
        // 处理不同格式的日期
        if (planDateStr && planDateStr.includes('T')) {
          planDateStr = planDateStr.split('T')[0];
        }
        // 格式化为 MM-DD 进行比较
        if (planDateStr.includes('-')) {
          const parts = planDateStr.split('-');
          if (parts.length >= 3) {
            // YYYY-MM-DD -> MM-DD
            planDateStr = `${parts[1]}-${parts[2]}`;
          }
        }
        return planDateStr === date;
      });
      
      console.log(`${date} 的膳食计划:`, dayPlans);
      
      // 计算当天总卡路里
      const dayTotal = dayPlans.reduce((sum, plan) => {
        return sum + (plan.calories || 0);
      }, 0);
      
      console.log(`${date} 计算得到的膳食计划总卡路里: ${dayTotal}`);
      calorieIntake.push(dayTotal);
    }
    
    // 更新数据，即使所有数据都是0也使用真实数据
    console.log('使用真实膳食计划卡路里数据:', calorieIntake);
    data.weeklyCalorieData = {
      dates,
      calorieIntake
    };
    
  } catch (error) {
    console.error('获取膳食计划数据时发生错误:', error);
    
    // 在错误情况下，使用0值数组确保图表能正常显示
    const dates = generateCurrentWeekDates();
    const zeroData = dates.map(() => 0);
    
    console.log('发生错误，使用0值数据:', zeroData);
    
    data.weeklyCalorieData = {
      dates,
      calorieIntake: zeroData
    };
  }
};

// 初始化一周卡路里趋势图
const initCalorieTrendChart = () => {
  const chartDom = document.getElementById('calorieTrendChart');
  if (!chartDom) return;
  
  data.charts.calorieTrend = echarts.init(chartDom);
  
  const option = {
    tooltip: {
        trigger: 'axis',
        formatter: function(params) {
          return params[0].name + ' 日期<br/>' +
                 '膳食计划卡路里合计: ' + params[0].value + ' 卡路里';
        }
      },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '15%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.weeklyCalorieData.dates,
      name: '日期',
      nameLocation: 'middle',
      nameGap: 40,
      axisLabel: {
        rotate: 0,
        interval: 0,
        formatter: function(value) {
          // 格式化显示为 MM-DD
          return value;
        },
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      name: '卡路里合计',
      nameLocation: 'middle',
      nameGap: 50,
      axisLabel: {
        formatter: '{value} 卡',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#e0e0e0'
        }
      }
    },
    series: [
      {
        name: '每日卡路里合计',
        type: 'line',
        smooth: true,
        data: data.weeklyCalorieData.calorieIntake,
        lineStyle: {
          color: '#5470c6',
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(84, 112, 198, 0.6)' },
            { offset: 1, color: 'rgba(84, 112, 198, 0.1)' }
          ])
        },
        itemStyle: {
          color: '#5470c6',
          borderColor: '#fff',
          borderWidth: 2
        },
        markLine: {
          silent: true,
          lineStyle: {
            color: '#999'
          },
          data: [
            {
              yAxis: 2000,
              label: {
                formatter: '每日推荐摄入量',
                position: 'insideEndTop'
              }
            }
          ]
        },
        // 在每个数据点上显示具体数值
        label: {
          show: true,
          position: 'top',
          formatter: '{c} 卡',
          fontSize: 10,
          color: '#333',
          fontWeight: 'bold',
          distance: 10,
          // 避免标签重叠
          rotate: 0,
          // 确保标签在图表区域内
          overflow: 'truncate',
          width: 80
        },
      }
    ],
    animation: true,
    animationDuration: 1500,
    animationEasing: 'cubicOut',
    // 添加图表背景色
    backgroundColor: 'transparent'
  };
  
  data.charts.calorieTrend.setOption(option);
};

// 响应式处理
const handleResize = () => {
  if (data.charts.calorieTrend) {
    data.charts.calorieTrend.resize();
  }
};

// 组件挂载时初始化图表
onMounted(async () => {
  // 获取膳食计划卡路里数据
  await fetchCalorieDataFromMealPlan();
  
  // 等待DOM渲染完成
  setTimeout(() => {
    initCalorieTrendChart();
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', handleResize);
  }, 100);
});

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (data.charts.calorieTrend) {
    data.charts.calorieTrend.dispose();
  }
});
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin: 0;
  width: 100%;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

.chart-container {
  width: 100%;
  height: 400px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .home-container {
    padding: 10px;
  }
  
  .chart-card {
    padding: 15px;
  }
  
  .chart-title {
    font-size: 16px;
  }
  
  .chart-container {
    height: 300px;
  }
}
</style>