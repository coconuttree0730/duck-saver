FROM rabbitmq:3.13-management

# 安装延迟消息插件（ADR 0007：通知类定时触发用延迟消息精确触发）
ADD https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.13.0/rabbitmq_delayed_message_exchange-3.13.0.ez /opt/rabbitmq/plugins/

RUN chmod 644 /opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-3.13.0.ez \
    && rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange
