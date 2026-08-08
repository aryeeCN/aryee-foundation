package cn.aryee.examples.database.blocking.service;

import cn.aryee.database.infrastructure.blocking.jpa.service.BaseJpaDataService;
import cn.aryee.examples.database.blocking.entity.User;
import cn.aryee.examples.database.blocking.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 用户服务（JPA 模式）
 * 继承 BaseJpaDataService 自动获得 BaseDataService 全部能力
 * 仅在激活 "test" profile 时创建
 *
 * @author Aryee
 * @since 1.0.0
 */
@Service
@Profile("test")
public class UserService extends BaseJpaDataService<User, Long> {

    public UserService(UserRepository repository) {
        super(repository, User.class);
    }
}
