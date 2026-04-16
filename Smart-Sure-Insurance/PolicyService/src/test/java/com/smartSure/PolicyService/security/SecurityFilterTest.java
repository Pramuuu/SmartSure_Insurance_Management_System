package com.smartSure.PolicyService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Filter Unit Tests")
class SecurityFilterTest {

    // ════════════════════════════════════════════════════════════════════════════
    // HeaderAuthenticationFilter
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HeaderAuthenticationFilter")
    class HeaderAuthFilterTests {

        @InjectMocks private HeaderAuthenticationFilter filter;
        @Mock private HttpServletRequest  request;
        @Mock private HttpServletResponse response;
        @Mock private FilterChain         filterChain;

        @BeforeEach  void clearBefore() { SecurityContextHolder.clearContext(); }
        @AfterEach   void clearAfter()  { SecurityContextHolder.clearContext(); }

        @Test
        @DisplayName("should set ROLE_CUSTOMER auth when valid headers present")
        void validHeaders_setsAuthentication() throws Exception {
            when(request.getHeader("X-User-Id")).thenReturn("42");
            when(request.getHeader("X-User-Role")).thenReturn("CUSTOMER");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(42L);
            assertThat(auth.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_CUSTOMER");
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("should auto-prefix ROLE_ when missing")
        void roleWithoutPrefix_addsPrefix() throws Exception {
            when(request.getHeader("X-User-Id")).thenReturn("1");
            when(request.getHeader("X-User-Role")).thenReturn("ADMIN"); // no ROLE_ prefix

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should not block request when headers are missing — just no auth set")
        void missingHeaders_noAuthButChainContinues() throws Exception {
            when(request.getHeader("X-User-Id")).thenReturn(null);
            when(request.getHeader("X-User-Role")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response); // chain continues
        }

        @Test
        @DisplayName("should continue chain even when X-User-Id is not a valid number")
        void invalidUserId_continuesChain() throws Exception {
            when(request.getHeader("X-User-Id")).thenReturn("not-a-number");
            when(request.getHeader("X-User-Role")).thenReturn("CUSTOMER");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("should set ROLE_ADMIN auth for admin user")
        void adminUser_setsAdminAuth() throws Exception {
            when(request.getHeader("X-User-Id")).thenReturn("1");
            when(request.getHeader("X-User-Role")).thenReturn("ROLE_ADMIN");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getPrincipal()).isEqualTo(1L);
            assertThat(auth.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_ADMIN");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // InternalRequestFilter
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InternalRequestFilter")
    class InternalRequestFilterTests {

        @InjectMocks private InternalRequestFilter filter;
        @Mock private HttpServletRequest  request;
        @Mock private HttpServletResponse response;
        @Mock private FilterChain         filterChain;

        private static final String VALID_SECRET = "TEST_SECRET_ABC123";

        @BeforeEach
        void injectSecret() {
            ReflectionTestUtils.setField(filter, "internalSecret", VALID_SECRET);
        }

        @Test
        @DisplayName("should allow request with correct internal secret")
        void correctSecret_allowsRequest() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/policies/purchase");
            when(request.getHeader("X-Internal-Secret")).thenReturn(VALID_SECRET);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("should block with 403 when secret is wrong")
        void wrongSecret_blocks403() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/policies/purchase");
            when(request.getHeader("X-Internal-Secret")).thenReturn("WRONG");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should block with 403 when secret header is absent")
        void missingSecret_blocks403() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/policies/purchase");
            when(request.getHeader("X-Internal-Secret")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should bypass secret check for /swagger-ui paths")
        void swaggerPath_bypasses() throws Exception {
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("should bypass secret check for /v3/api-docs paths")
        void apiDocsPath_bypasses() throws Exception {
            when(request.getRequestURI()).thenReturn("/v3/api-docs/swagger-config");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}