<template>
  <DataLayout>
    <template v-slot:header>
      <el-form inline>
        <el-form-item>
          <DataSelect
            v-model="icmReportStatistc"
            data-url="/exchange/project/myproject"
            data-value-field="name"
            data-text-field="name"
            includeAll
          ></DataSelect>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search1()">{{
            $t("application.SearchData")
          }}</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click.native="exportData">{{
            $t("application.ExportExcel")
          }}</el-button>
        </el-form-item>
        <el-form-item>
          <AddCondition
            @change="searchItem"
            v-model="advCondition"
            v-bind:typeName="typeName"
            :inputValue="advCondition"
            :inputType="hiddenInput"
          ></AddCondition>
        </el-form-item>
      </el-form>
    </template>
    <template v-slot:main="{ layout }">
      <el-row>
        <el-col :span="24">
          <DataGrid
            ref="mainDataGrid"
            data-url="/dc/getDocumentsICMUnion"
            :isShowMoreOption="false"
            :isshowOption="true"
            :isshowCustom="false"
            :isshowicon="false"
            gridViewName="ICMOpenGrid"
            condition="TYPE_NAME='' and C_PROJECT_NAME = '@project'"
            :tableHeight="layout.height - 210"
          ></DataGrid>
        </el-col>
      </el-row>
    </template>
  </DataLayout>
</template>
<script type="text/javascript">
import ShowProperty from "@/components/ShowProperty";
import DataGrid from "@/components/DataGrid";
import DataSelect from "@/components/ecm-data-select";
import DataLayout from "@/components/ecm-data-layout";
import AddCondition from "@/views/record/AddCondition.vue";
import ExcelUtil from "@/utils/excel.js";
import FileSaver from "file-saver";
import XLSX from "xlsx";
export default {
  name: "ICMOpenP",
  data() {
    return {
      icmReportStatistc: "",
      hiddenInput: "hidden",
      typeName: "ICM",
      advCondition: "",
    };
  },

  methods: {
    search1() {
      let _self = this;

      var k1 = "";

      if (
        _self.icmReportStatistc != undefined &&
        _self.icmReportStatistc != "所有项目"
      ) {
        k1 += "C_PROJECT_NAME in (" + _self.icmReportStatistc + ")";
      }

      let user = this.currentUser();
      if (user.userType == 2 && user.company != null) {
        k1 += " AND C_COMPANY='" + user.company + "'";
      }

      k1 += " AND C_ITEM1_DATE is not null AND C_ITEM2_DATE is not null";

      if (_self.advCondition != undefined && _self.advCondition.length > 0) {
        k1 += " and " + _self.advCondition;
      }

      _self.$nextTick(()=>{
         _self.$refs.mainDataGrid.condition = k1;
        _self.$refs.mainDataGrid.currentPage = 1;
        _self.$refs.mainDataGrid.loadGridInfo();
        _self.$refs.mainDataGrid.loadGridData();
      })
    },

    searchItem() {
      this.search1()
    },

    exportData() {
      let dataUrl = "/exchange/doc/export";
      var fileDate = new Date();
      let fileDateStr =
        fileDate.getFullYear() +
        "" +
        fileDate.getMonth() +
        "" +
        fileDate.getDate();
      let params = {
        gridName: "ICMExchangeList",
        lang: "zh-cn",
        condition: this.$refs.mainDataGrid.condition,
        filename: "ICM_OpenReport_" + fileDateStr + ".xlsx",
        sheetname: "ICM_OpenReport",
      };
      ExcelUtil.export(params);
    },

    onSearchConditionChange: function (val) {
      this.search(val);
    },
  },

  components: {
    ShowProperty: ShowProperty,
    DataGrid: DataGrid,
    DataSelect: DataSelect,
    DataLayout: DataLayout,
    AddCondition: AddCondition,
  },
};
</script>
<style scoped>
.el-form-item {
  margin-bottom: 0px;
}
</style>
