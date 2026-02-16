# BSHongbao 更新记录
## **版本 2.0.0**（2026-02-15）
  - 新增功能
    - 无
  - 优化改进
    - 添加bshongbao_claim命令，防止按下领取红包时出现弹窗
    - 优化插件性能
    - 使用minimessage替换部分聊天消息，提升用户体验
  - 修复问题
    - 无
  - 计划
    - 允许控制台使用某些指令
  - 配置文件变动
    - ```yaml
      errors:
      no-permission: "&c您没有权限使用此命令！"
      #此处省略部分配置
      packet-not-found: "&c红包不存在或已过期！"

      reload-error: "&c配置文件重载失败,请查看控制台输出信息!" #新增
    
      # 成功消息 Success Messages
      success:
      packet-created: "&a成功创建红包！总金额: &6{amount} &a份数: &e{count}"
      packet-claimed: "&a恭喜！您领取了 &6{amount} &a的红包！"
      
      reload-success: "&a配置文件已重新加载！" #新增
      ```