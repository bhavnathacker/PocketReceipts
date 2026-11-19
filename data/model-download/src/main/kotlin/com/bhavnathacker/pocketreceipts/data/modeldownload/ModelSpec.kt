package com.bhavnathacker.pocketreceipts.data.modeldownload

/**
 * The model file to download and how to recognise it.
 *
 * @property url Where to download it from, pinned to one revision.
 * @property fileName The verified file's name in the models directory.
 * @property sizeBytes The file's exact size.
 * @property sha256 The file's SHA-256 checksum, lower-case hex.
 */
internal data class ModelSpec(
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
) {
    companion object {
        /**
         * Gemma 4 E2B for LiteRT-LM (CPU and GPU), Apache 2.0, from Hugging Face
         * `litert-community/gemma-4-E2B-it-litert-lm` at revision `2d74ac41`.
         */
        val GEMMA_4_E2B = ModelSpec(
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
                "2d74ac41a33d7da82f39f7d5720a04c68514a103/gemma-4-E2B-it.litertlm",
            fileName = "gemma-4-E2B-it.litertlm",
            sizeBytes = 2_588_147_712L,
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
        )
    }
}
