package com.jeontongju.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.controller.KakaoController;
import com.jeontongju.payment.enums.temp.MemberRoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = {KakaoController.class})
public class ControllerTestUtil {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected void checkControllerSuccessConditions(String url, Long memberId, MemberRoleEnum memberRole, Object data, ResultMatcher resultMatcher) throws Exception {
        mockMvc
                .perform(
                        post(url)
                                .header("memberId",memberId)
                                .header("memberRole", memberRole)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(data)))
                .andDo(print())
                .andExpect(resultMatcher);
    }

    protected void checkControllerFailConditions(String url, Long memberId, MemberRoleEnum memberRole, Object data, String msg, ResultMatcher resultMatcher) throws Exception {
        mockMvc
                .perform(
                        post(url)
                                .header("memberId",memberId)
                                .header("memberRole", memberRole)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(data)))
                .andDo(print())
                .andExpect(jsonPath("$.message").value(msg))
                .andExpect(resultMatcher);
    }
}
