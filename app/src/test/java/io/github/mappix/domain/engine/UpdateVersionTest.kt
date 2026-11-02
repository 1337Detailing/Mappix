package io.github.mappix.domain.engine

import io.github.mappix.data.update.GithubUpdateRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test fun `newer stable release is detected`() {
        assertTrue(GithubUpdateRepository.isNewer("v0.2.1", "0.2.0"))
        assertTrue(GithubUpdateRepository.isNewer("v1.0.0", "0.9.99"))
        assertTrue(GithubUpdateRepository.isNewer("2.10.0", "2.9.9"))
    }

    @Test fun `older invalid and matching versions are ignored`() {
        assertFalse(GithubUpdateRepository.isNewer("v0.1.9", "0.2.0"))
        assertFalse(GithubUpdateRepository.isNewer("v0.2.0", "0.2.0"))
        assertFalse(GithubUpdateRepository.isNewer("broken", "0.2.0"))
    }
}
