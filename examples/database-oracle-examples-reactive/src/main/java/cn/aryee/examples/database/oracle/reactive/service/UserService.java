package cn.aryee.examples.database.oracle.reactive.service;

import cn.aryee.database.infrastructure.reactive.r2dbc.service.BaseR2dbcDataService;
import cn.aryee.examples.database.oracle.reactive.entity.User;
import cn.aryee.examples.database.oracle.reactive.repository.UserRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户服务（Reactive - Oracle）
 * 继承 {@link BaseR2dbcDataService} 自动获得全部能力。
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
