package com.example123.demo.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example123.demo.domain.PopulationData;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Service
public class PopulationDataService {

    @Value("${file.path.population:data.csv}")
    private String csvFilePath;

    public List<PopulationData> loadPopulationData() throws IOException {
        CsvMapper mapper = new CsvMapper();
        
        // タブ区切りとヘッダー行の存在を指定
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator('\t');

        try (
            FileInputStream fis = new FileInputStream(csvFilePath);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr)
        ) {
            // 不要な先頭2行を読み飛ばす
            reader.readLine();
            reader.readLine();

            // 3行目からヘッダーとして読み込み、データをマッピングする
            MappingIterator<PopulationData> it = mapper
                .readerFor(PopulationData.class)
                .with(schema)
                .readValues(reader);
            
            List<PopulationData> data = it.readAll();
            data.forEach(d -> {
            	d.setNo(cleanNumber(d.getNo()));
            	d.setYear(cleanNumber(d.getYear()));
            	d.setPopulationOver15(cleanNumber(d.getPopulationOver15()));
            	d.setPopulationUnder15(cleanNumber(d.getPopulationUnder15()));
            	d.setPopulationOver65(cleanNumber(d.getPopulationOver65()));
            	d.setPopulationOver75(cleanNumber(d.getPopulationOver75()));
            	d.setTotalPopulationJapaneseResident(cleanNumber(d.getTotalPopulationJapaneseResident()));
            	d.setTotalPopulationEstimate(cleanNumber(d.getTotalPopulationEstimate()));
            	d.setTotalPopulationResidentRegister(cleanNumber(d.getTotalPopulationResidentRegister()));
            	d.setLaborForcePopulation(cleanNumber(d.getLaborForcePopulation()));
            });
            return data;
           } catch (IOException e) {
            throw new RuntimeException("CSVファイルの読み込みに失敗しました: " + csvFilePath, e);
           }
          }
         
          private String cleanNumber(String number) {
           if (number == null || number.trim().isEmpty()) {
            return "0";
           }
           return number.replace(",", "");
          }
         }