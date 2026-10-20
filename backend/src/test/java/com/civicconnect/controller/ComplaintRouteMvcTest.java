package com.civicconnect.controller;

import com.civicconnect.dto.ComplaintResponse;
import com.civicconnect.service.ComplaintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the public API route including the configured /api context path. */
@WebMvcTest(ComplaintController.class)
@AutoConfigureMockMvc(addFilters = false)
class ComplaintRouteMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @Test
    void listComplaints_isAvailableAtApiV1Complaints() throws Exception {
        when(complaintService.getAllComplaints(any())).thenReturn(new PageImpl<>(List.<ComplaintResponse>of()));

        mockMvc.perform(get("/api/v1/complaints").contextPath("/api"))
                .andExpect(status().isOk());
    }
}
