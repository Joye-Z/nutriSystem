<template>
  <div class="background-image">
    <div style="font-weight: bold; font-size: 35px; text-align: center; color: darkgreen; padding-top: 50px">健康食谱管理与膳食规划系统</div>
    <div class="login-box">
      <div style="font-weight: bold; font-size: 28px; text-align: center; margin-bottom: 30px; color: #057748">登 录</div>
      <el-form :model="data.form" ref="formRef" :rules="data.rules" @submit.native.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input :prefix-icon="User" size="large" v-model="data.form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input :prefix-icon="Lock" size="large" v-model="data.form.password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item prop="role">
          <el-select size="large" style="width: 100%" v-model="data.form.role">
            <el-option value="USER" label="普通用户" style="color: #1fab89"></el-option>
            <el-option value="ADMIN" label="管理员" style="color: #1fab89"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
              size="large"
              type="success"
              style="width: 100%"
              :disabled="showSlideVerify"
              @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: right;">
        还没有账号？请 <a href="/register" style="color: #057748">注册</a>
      </div>
    </div>
    <!-- 滑块验证 -->
    <el-card class="cover" v-if="showSlideVerify">
      <slide-verify
          class="silde_box"
          ref="block"
          :l="42"
          :r="10"
          :w="310"
          :h="155"
          :accuracy="5"
          :imgs="data.slideImgs"
          slider-text="向右滑动"
          @again="onAgain"
          @success="onSuccess"
          @fail="onFail"
          @refresh="onRefresh"
      ></slide-verify>
      <div>{{ msg }}</div>
    </el-card>
  </div>
</template>


<script setup>
import { ref, reactive } from 'vue';
import { User, Lock } from '@element-plus/icons-vue';
import request from '@/utils/request';
import { ElMessage } from 'element-plus';
import router from '@/router';
import SlideVerify from 'vue3-slide-verify';
import 'vue3-slide-verify/dist/style.css';
import img1 from "../assets/imgs/img1.jpg";
import img2 from "../assets/imgs/img2.jpg";
import img3 from "../assets/imgs/img3.jpg";
import img4 from "../assets/imgs/img4.jpg";

const msg = ref('');
const block = ref(null);
const showSlideVerify = ref(false);
const data = reactive({
  form: { username: '', password: '', role: 'USER' },
  loginData: {},
  slideImgs: [img1, img2, img3, img4],
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 10, message: '长度在3-10个字符', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 3, max: 10, message: '长度在3-10个字符', trigger: 'blur' }
    ]
  }
});
const formRef = ref(null);

const onAgain = () => {
  msg.value = "检测到非人为操作，请再试一次！";
  block.value?.refresh();
};

const onSuccess = () => {
  showSlideVerify.value = false; // 隐藏滑块验证
  performLogin(); // 调用登录方法
};

const onFail = () => {
  msg.value = "验证失败，请重新滑动";
  block.value?.refresh();
};

const handleLogin = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      showSlideVerify.value = true; // 显示滑块验证
    } else {
      ElMessage.error('请填写正确的账号和密码');
    }
  });
};

const performLogin = () => {
  request.post('/login', data.form).then((res) => {
    if (res.code === '200') {
      data.loginData = res.data;
      ElMessage.success("登录成功");
      router.push('/'); // 跳转到主页
      localStorage.setItem('system-user', JSON.stringify(res.data));
    } else {
      ElMessage.error(res.msg);
      showSlideVerify.value = false; // 重置滑块验证
    }
  }).catch(() => {
    ElMessage.error("网络错误，请稍后再试");
    showSlideVerify.value = false; // 重置滑块验证
  });
};

</script>

<style scoped>
.login-box {
  margin: 50px auto;
  width: 350px;
  padding: 40px 30px;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  background-color: rgba(255, 255, 255, 0.9);
}
.cover {
  width: fit-content;
  background-color: white;
  position: absolute;
  top:50%;
  left:50%;
  transform: translate(-50%, -50%);
  z-index: 1000;
}
.background-image {
  background-image: url('../../src/assets/imgs/background.jpg'); /* 替换为您的图片路径 */
  height: 100vh;
  overflow: hidden;
  position: relative;
  background-size: cover; /* 确保图片覆盖整个区域 */
  background-position: center; /* 图片居中显示 */
  background-blend-mode: multiply; /* 混合模式 */
  opacity: 0.9;
}
.silde_box {
  margin: 0 auto;
}
</style>