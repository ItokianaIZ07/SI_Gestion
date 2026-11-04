package com.si.rh.controller;

import com.si.rh.service.BesoinService;
import com.si.rh.service.ValidationBesoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BesoinControllerTest {
    private static final String EMAIL = "manager@example.test";

    @Mock
    private ValidationBesoinService validationBesoinService;

    @Mock
    private BesoinService besoinService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BesoinController(validationBesoinService, besoinService)).build();
    }

    @Test
    void directDecisionRequestByUnauthorizedUserReturnsForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle non autorisé"))
                .when(validationBesoinService)
                .prendreDecision(42, "ACCEPTE", null, null, null, EMAIL);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(new TestingAuthenticationToken(EMAIL, "unused"));

        mockMvc.perform(post("/api/rh/besoins/decision")
                        .principal(request.getUserPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idBesoin":42,"decisionCode":"ACCEPTE"}
                                """))
                .andExpect(status().isForbidden());

        verify(validationBesoinService).prendreDecision(42, "ACCEPTE", null, null, null, EMAIL);
    }
}
