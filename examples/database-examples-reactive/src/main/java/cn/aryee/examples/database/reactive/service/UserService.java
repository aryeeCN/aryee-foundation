package cn.aryee.examples.database.reactive.service;

import cn.aryee.database.infrastructure.reactive.r2dbc.service.BaseR2dbcDataService;
import cn.aryee.examples.database.reactive.entity.User;
import cn.aryee.examples.database.reactive.repository.UserRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户服务（Reactive）
 * 继承 {@link BaseR2dbcDataService} 自动获得 {@code ReactiveBaseDataService} 全部能力。
 *
 * <p>通过构造器注入 Spring Data 自动生成的 {@link UserRepository} 代理和 {@link R2dbcEntityTemplate}，
 * 无需手动编写 Config 注册 Bean。</p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Service
public class UserService extends BaseR2dbcDataService<User, Long> {

    public UserService(UserRepository userRepository, R2dbcEntityTemplate entityTemplate) {
        super(userRepository, entityTemplate, User.class);
    }
}
