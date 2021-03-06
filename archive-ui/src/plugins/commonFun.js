
import Vue from 'vue'
//逻辑删除
Vue.prototype.logicallyDel=function(items,callback){
    let _self=this;
    if(!items||items.length==0){
        _self.$message({
            showClose: true,
            message: _self.$t("message.PleaseSelectOneOrMoreData"),
            duration: 2000,
            type: "error"
          });
          return;
    }
    
    let m=new Map();
    let ids=new Array();
    items.forEach(element => {
        ids.push(element.ID);
    });
    m.set("ids",ids);
    axios
    .post("/dc/logicallyDel", JSON.stringify(m))
    .then(function(response) {
      if(response.data.code!=1){
        _self.$message({
          showClose: true,
          message: _self.$t("message.deleteFailured"),
          duration: 2000,
          type: "error"
        });
        return;
      }else{
        _self.$message({
          showClose: true,
          message: _self.$t("message.deleteSuccess"),
          duration: 2000,
          type: "success"
        });
        if(callback){
          callback();
        }
      }
      
    });
}
//修改数据
Vue.prototype.updateData=function(param,callback){
    let _self=this;
    axios
    .post("/dc/updateData", JSON.stringify(param))
    .then(function(response) {
      if(response.data.code!=1){
        _self.$message({
          showClose: true,
          message: _self.$t("message.updateFailured"),
          duration: 2000,
          type: "error"
        });
        return;
      }else{
        _self.$message({
          showClose: true,
          message: _self.$t("message.updateSuccess"),
          duration: 2000,
          type: "success"
        });
        if(callback){
          callback();
        }
      }
      
    });
}