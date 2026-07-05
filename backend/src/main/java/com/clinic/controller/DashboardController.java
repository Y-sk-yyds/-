package com.clinic.controller;

import com.clinic.common.Result;
import com.clinic.dto.DashboardVO;
import com.clinic.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器——提供首页统计数据聚合接口
 * <p>
 * 路径前缀: /api/dashboard
 * 访问权限: 所有已登录用户
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取仪表盘数据
     * <p>
     * GET /api/dashboard
     * <p>
     * 返回: 四项统计卡片 + 7日趋势 + 科室分布 + 待办提醒
     */
    @GetMapping
    public Result<DashboardVO> dashboard() {
        return Result.ok(dashboardService.build());
    }
}
