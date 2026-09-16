package com.example.travel_planner.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@Getter
public class StatusCode {
    HttpStatus statusCode;
    Object data;
    String msg;


    public StatusCode(HttpStatus statusCode, String msg){
        this.statusCode = statusCode;
        this.msg = msg;
    }

    public StatusCode(HttpStatus statusCode, Object data, String msg){
        this.statusCode = statusCode;
        this.data = data;
        this.msg = msg;
    }

        public ResponseEntity<?> sendResponse(){
            Map<String, Object> data = new HashMap<>();
            if(this.data != null) data.put("data", this.data);
            data.put("msg", this.msg);
            return new ResponseEntity<>(data, this.statusCode);
    }
}
