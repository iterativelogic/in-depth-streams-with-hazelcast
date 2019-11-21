package com.hazelcast.training.streams.client;

import com.github.javafaker.Company;
import com.github.javafaker.Faker;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.*;

import java.io.IOException;
import java.io.Serializable;

public class Merchant implements Serializable {
    private int merchantId;
    private String  merchantName;
    private String industry;

//    public static int CLASSID;
//    public static int VERSION;

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }


    public int getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(int merchantId) {
        this.merchantId = merchantId;
    }

    @Override
    public String toString() {
        return "Merchant{" +
                "merchantId=" + merchantId +
                ", merchantName='" + merchantName + '\'' +
                ", industry='" + industry + '\'' +
                '}';
    }

    private static Faker faker = new Faker();

    public static Merchant fake(int id) {
        Merchant result = new Merchant();
        Company company = faker.company();
        result.setMerchantName(company.name());
        result.setIndustry(company.industry());
        result.setMerchantId(id);
        return result;
    }

    public static void main(String []args){
        for(int i=0; i < 10; ++i){
            Merchant m = Merchant.fake(i);
            System.out.println(">>> " + m);
        }
    }


//    @Override
//    public void writePortable(PortableWriter portableWriter) throws IOException {
//        portableWriter.writeInt("merchantId", this.merchantId);
//        portableWriter.writeUTF("merchantName", this.merchantName);
//        portableWriter.writeUTF("industry", this.industry);
//    }
//
//    @Override
//    public void readPortable(PortableReader portableReader) throws IOException {
//        this.merchantId = portableReader.readInt("merchantId");
//        this.merchantName = portableReader.readUTF("merchantName");
//        this.industry = portableReader.readUTF("industry");
//    }
//
//    @Override
//    public int getFactoryId() {
//        return DefaultPortableFactory.ID;
//    }
//
//    @Override
//    public int getClassId() {
//        return CLASSID;
//    }
//    @Override
//    public int getClassVersion() {
//        return VERSION;
//    }
}