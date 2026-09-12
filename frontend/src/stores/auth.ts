import {defineStore} from "pinia";
import {getMe} from "@/api/auth";
import type {UserProfile} from "@/types";

function readStoredUser():UserProfile | null{
  try{
    const value=localStorage.getItem("petclinic-user");
    return value?JSON.parse(value):null;
  }catch {
    localStorage.removeItem("petclinic-user");
    return null;
  }
}

// defineStore第一个参数是这个pinia store的唯一id，内部识别码
//defineStore结构类似java的类，成员变量（state），查询/计算方法(getters)，业务方法(actions)，但pinia是实时的
export const useAuthStore = defineStore("auth",{
  //箭头函数，相当于lambda表达式，TODO它可以使用外层this，不需要定义函数式接口
  state:()=>{
    const user = readStoredUser();
    return{
      token:localStorage.getItem("petclinic-token") || "",
      user,
      //user存在，使用user.authorities放进权限集合中，不存在使用[]
      permissions:user?.authorities??[],
      restored:false,
    };
  },

  getters:{
    hasAuthority:(state) => (code:string)=>
        state.permissions.includes(code),
    hasRole:(state)=>(role:"ADMIN"|"STAFF"|"OWNER")=>
        state.permissions.includes(`ROLE_${role}`),
  },

  actions:{
    //TODO刚刚登录成功时调用
    //登录成功后端返回的结果里面有user和token
    setSession(token: string,user:UserProfile){
      const normalizedUser={
        ...user,
        //??运算符：左边为null或者undefine时使用右边默认值。TODO对比|| 当左边为0,空字符串，false时也是使用默认值
        authorities:user.authorities??[],
      };

      this.token=token;
      this.user=normalizedUser;
      this.permissions=normalizedUser.authorities;
      this.restored=true;

      localStorage.setItem("petclinic-token",token);
      localStorage.setItem("petclinic-user",JSON.stringify(normalizedUser));
    },

    /**
     * TODO刷新浏览器时使用，
     * 原因为，刷新后js内存中的pinia状态会消失，但localStore里面还保留token
     * 不知道保存的token是否过期，调用getMe向后端确认
     */
    async restoreSession() {
      //没身份证，直接返回，TODO restored含义为是否检查过登状态
      if(!this.token){
        this.restored=true;
        return;
      }

      try{
        //getMe其实就是把旧token传回后端进行验证是否过期
        const response = await getMe();
        //浏览器js重新保存状态
        this.setSession(this.token,response.data);
      }catch {
        this.logout();
      }finally {
        this.restored=true;
      }
    },

    logout(){
      this.token="";
      this.user=null;
      this.permissions=[];
      this.restored=true;

      localStorage.removeItem("petclinic-token");
      localStorage.removeItem("petclinic-user");
    },
  },
});