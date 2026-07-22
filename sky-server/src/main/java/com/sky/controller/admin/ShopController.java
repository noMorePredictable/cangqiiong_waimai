package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("admin")
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {
    /*设置店铺状态*/
    @Autowired
    private RedisTemplate redisTemplate;


    @PutMapping("/{status}")
    @ApiOperation("设置店铺状态")
    public Result setStatus(@PathVariable Integer status){

        log.info("设置店铺状态为:{}",status==1?"营业中":"打烊中");
        redisTemplate.opsForValue().set("status",status);
        return Result.success();
    }
    /*
    * 获取店铺状态
    * */
    @GetMapping("/status")
    public Result<Integer>getStatus(){
        Integer status = (Integer)redisTemplate.opsForValue().get("status");
        log.info("获取店铺状态为:{}",status==1?"营业中":"打烊中");
        return Result.success();
    }



}
