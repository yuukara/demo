package com.example123.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

// Lombokの@Dataアノテーションで、getter/setter、toString、equals、hashCodeメソッドを自動生成します。
@Data
// Jackson CSVライブラリがCSVの列を正しくマッピングするための順序を指定します。
@JsonPropertyOrder({
    "No", "都道府県", "年度", "15歳以上の人口【人】", "15歳未満の人口【人】", "65歳以上の人口【人】", "75歳以上の人口【人】",
    "人口総数（日本人）｜住民基本台帳【人】", "人口総数｜人口推計【人】", "人口総数｜住民基本台帳【人】", "労働力人口【人】", "女性割合【%】", "男性割合【%】"
})
public class PopulationData {

    @JsonProperty("No")
    private String no;
   
    @JsonProperty("都道府県")
    private String prefecture;
   
    @JsonProperty("年度")
    private String year;

    @JsonProperty("15歳以上の人口【人】")
    private String populationOver15;
   
    @JsonProperty("15歳未満の人口【人】")
    private String populationUnder15;
   
    @JsonProperty("65歳以上の人口【人】")
    private String populationOver65;
   
    @JsonProperty("75歳以上の人口【人】")
    private String populationOver75;
   
    // 元のCSVで2つあった「人口総数｜住民基本台帳【人】」の一方
    @JsonProperty("人口総数（日本人）｜住民基本台帳【人】")
    private String totalPopulationJapaneseResident;
   
    @JsonProperty("人口総数｜人口推計【人】")
    private String totalPopulationEstimate;
   
    // 元のCSVで2つあった「人口総数｜住民基本台帳【人】」のもう一方
    @JsonProperty("人口総数｜住民基本台帳【人】")
    private String totalPopulationResidentRegister;
   
    @JsonProperty("労働力人口【人】")
    private String laborForcePopulation;

    @JsonProperty("女性割合【%】")
    private double femalePercentage;

    @JsonProperty("男性割合【%】")
    private double malePercentage;
}
