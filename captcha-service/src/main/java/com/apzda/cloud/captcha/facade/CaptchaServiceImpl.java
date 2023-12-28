/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.captcha.facade;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.captcha.Captcha;
import com.apzda.cloud.captcha.ValidateStatus;
import com.apzda.cloud.captcha.config.CaptchaConfig;
import com.apzda.cloud.captcha.config.CaptchaConfigProperties;
import com.apzda.cloud.captcha.proto.*;
import com.apzda.cloud.captcha.storage.CaptchaStorage;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.utils.I18nHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final CaptchaConfig captchaConfig;

    private final CaptchaConfigProperties properties;

    private final CaptchaStorage captchaStorage;

    @Override
    public CreateRes create(CreateReq request) {
        val builder = CreateRes.newBuilder();
        if (GsvcContextHolder.getRequest().isPresent()) {
            val req = GsvcContextHolder.getRequest().get();
            val remoteAddr = req.getRemoteAddr();
            int count = captchaStorage.getIpCount(remoteAddr);
            if (count > properties.getMaxCount()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            }
        }
        val captchaProvider = captchaConfig.getCaptchaProvider();
        if (captchaProvider == null) {
            builder.setErrCode(500);
            builder.setErrMsg(I18nHelper.t("captcha.provider.404"));
        }
        else {
            val uuid = request.getUuid();
            var width = request.getWidth();
            var height = request.getHeight();
            if (width <= 0) {
                width = properties.getWidth();
            }
            if (height <= 0) {
                height = properties.getHeight();
            }
            try {
                val captcha = captchaProvider.create(uuid, width, height, properties.getTimeout());
                builder.setType(captchaProvider.getId());
                builder.setId(captcha.getId());
                builder.setCaptcha(captcha.getCode());
                builder.setExpireTime(captcha.getExpireTime());
            }
            catch (Exception e) {
                log.warn("Cannot create captcha - {}", e.getMessage());
                builder.setErrCode(500);
                builder.setErrMsg(I18nHelper.t("captcha.provider.500"));
            }
        }
        return builder.build();
    }

    @Override
    public ValidateRes validate(ValidateReq request) {
        val uuid = request.getUuid();
        val id = request.getId();
        val code = request.getCode();
        val builder = ValidateRes.newBuilder();
        val captchaProvider = captchaConfig.getCaptchaProvider();
        val removeOnInvalid = properties.isRemoveOnInvalid();
        if (captchaProvider == null) {
            builder.setErrCode(1);
            builder.setErrMsg(I18nHelper.t("captcha.invalid"));
        }
        else {
            try {
                val validate = captchaProvider.validate(uuid, id, code, removeOnInvalid);
                if (validate == ValidateStatus.OK) {
                    builder.setErrCode(0);
                    val captcha = new Captcha();
                    captcha.setId(id);
                    captcha.setCode(Captcha.VERIFIED);
                    captcha.setExpireTime(DateUtil.currentSeconds() + properties.getTimeout().toSeconds());
                    captchaStorage.save(uuid, captcha);
                    return builder.build();
                }
                else if (validate == ValidateStatus.EXPIRED) {
                    builder.setErrCode(2);
                    builder.setErrMsg(I18nHelper.t("captcha.expired"));
                    builder.setReload(true);
                    return builder.build();
                }
                else if (removeOnInvalid || captchaStorage.getErrorCount(uuid, id) > properties.getMaxTryCount()) {
                    builder.setReload(true);
                }
            }
            catch (Exception e) {
                log.warn("Error occurred while validate captcha(uuid: {}, id: {}) - {}", uuid, id, e.getMessage());
            }
        }
        builder.setErrCode(1);
        builder.setErrMsg(I18nHelper.t("captcha.invalid"));
        return builder.build();
    }

    @Override
    public GsvcExt.CommonRes check(CheckReq request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        builder.setErrCode(0);
        val uuid = request.getUuid();
        val id = request.getId();
        val captcha = new Captcha();
        captcha.setId(id);
        try {
            val ca = captchaStorage.load(uuid, captcha);
            if (ca == null) {
                throw new IllegalStateException("Cannot load captcha from storage");
            }
            val now = DateUtil.currentSeconds();
            if (now > ca.getExpireTime()) {
                builder.setErrCode(2);
                builder.setErrMsg(I18nHelper.t("captcha.expired"));
                return builder.build();
            }
            else if (!Captcha.VERIFIED.equals(ca.getCode())) {
                builder.setErrCode(1);
                builder.setErrMsg(I18nHelper.t("captcha.invalid"));
            }
        }
        catch (Exception e) {
            log.error("Cannot load captcha(uuid: {}, id: {}) - {}", uuid, id, e.getMessage());
            builder.setErrCode(1);
            builder.setErrMsg(I18nHelper.t("captcha.invalid"));
        }
        return builder.build();
    }

}
