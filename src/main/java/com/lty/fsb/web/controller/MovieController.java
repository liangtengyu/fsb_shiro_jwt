package com.lty.fsb.web.controller;

import com.lty.fsb.common.domain.FsbConstant;
import com.lty.fsb.common.domain.FsbResponse;
import com.lty.fsb.common.exception.FsbGlobalException;
import com.lty.fsb.common.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("movie")
public class MovieController {

    private String message;

    @GetMapping("hot")
    public FsbResponse getMoiveHot() throws FsbGlobalException {
        try {
            String data = HttpUtil.sendSSLPost(FsbConstant.TIME_MOVIE_HOT_URL, "locationId=328");
            return new FsbResponse().data(data);
        } catch (Exception e) {
            message = "获取热映影片信息失败";
            log.error(message, e);
            throw new FsbGlobalException(message);
        }
    }

    @GetMapping("coming")
    public FsbResponse getMovieComing() throws FsbGlobalException {
        try {
            String data = HttpUtil.sendSSLPost(FsbConstant.TIME_MOVIE_COMING_URL, "locationId=328");
            return new FsbResponse().data(data);
        } catch (Exception e) {
            message = "获取即将上映影片信息失败";
            log.error(message, e);
            throw new FsbGlobalException(message);
        }
    }

    @GetMapping("detail")
    public FsbResponse getDetail(@NotBlank(message = "{required}") String id) throws FsbGlobalException {
        try {
            String data = HttpUtil.sendSSLPost(FsbConstant.TIME_MOVIE_DETAIL_URL, "locationId=328&movieId=" + id);
            return new FsbResponse().data(data);
        } catch (Exception e) {
            message = "获取影片详情失败";
            log.error(message, e);
            throw new FsbGlobalException(message);
        }
    }

    @GetMapping("comments")
    public FsbResponse getComments(@NotBlank(message = "{required}") String id) throws FsbGlobalException {
        try {
            String data = HttpUtil.sendSSLPost(FsbConstant.TIME_MOVIE_COMMENTS_URL, "movieId=" + id);
            return new FsbResponse().data(data);
        } catch (Exception e) {
            message = "获取影片评论失败";
            log.error(message, e);
            throw new FsbGlobalException(message);
        }
    }
}
