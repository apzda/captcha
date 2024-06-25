# Apzda Captcha

Now Supports:

1. Image: 图片验证码
2. Slider: 图片滑块验证码
3. Drag: 滑动验证码

## 全局配置

```properties
# 验证码类型,目前支持: image,slider,drag
apzda.cloud.captcha.provider=image
# 测试模式，默认为false. 为true时图片验证码固定为:A12b
apzda.cloud.captcha.props.test-mode=false
```

## Image: 图片验证码

```properties
apzda.cloud.captcha.provider=image
# 干扰图形类型，支持: line,shear和circle
apzda.cloud.captcha.props.type=line
# 自定义验证码字符，默认为26个字母（含大写）和10数字
apzda.cloud.captcha.props.codes=
# 验证码长度
apzda.cloud.captcha.props.length=4
# 干扰量
apzda.cloud.captcha.props.count=60
# 在小写敏感，默认为false
apzda.cloud.captcha.props.case-sensitive=false
```

## Slider: 图片滑块验证码

```properties
apzda.cloud.captcha.provider=slider
# 水印,默认无
apzda.cloud.captcha.props.watermark=
# 噪点数量
apzda.cloud.captcha.props.noise=1
# 容忍度
apzda.cloud.captcha.props.tolerant=5
```

## Drag: 滑动验证码

```properties
apzda.cloud.captcha.provider=drag
```

## 接口

1. [创建验证码接口](https://github.com/apzda/captcha/blob/6b09aadb23025eef4338c0f953ae6c28d057b1ec/captcha-proto/src/main/proto/captcha.proto#L44)
2. [校验验证码接口](https://github.com/apzda/captcha/blob/6b09aadb23025eef4338c0f953ae6c28d057b1ec/captcha-proto/src/main/proto/captcha.proto#L45)
