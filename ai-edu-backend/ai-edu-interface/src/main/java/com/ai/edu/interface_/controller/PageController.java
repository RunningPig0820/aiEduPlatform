package com.ai.edu.interface_.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器
 */
@Controller
public class PageController {

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 学生首页
     */
    @GetMapping("/student")
    public String studentHome() {
        return "pages/student/home";
    }

    /**
     * 老师首页
     */
    @GetMapping("/teacher")
    public String teacherHome() {
        return "pages/teacher/home";
    }

    /**
     * 家长首页
     */
    @GetMapping("/parent")
    public String parentHome() {
        return "pages/parent/home";
    }
}