package com.example.bandlink.controller; import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.GetMapping;
@Controller public class PageController{@GetMapping({"/","/posts"}) public String posts(){return "posts";}}
