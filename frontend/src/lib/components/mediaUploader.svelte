<script>
    import { onDestroy, createEventDispatcher, onMount } from 'svelte';
    
    export let mediaFiles = [];
    export let errors = { media: '' };
    
    const dispatch = createEventDispatcher();
    let uniqueId = '';

    onMount(() => {
      // Generate a unique ID for this instance
      uniqueId = `media-upload-${Math.random().toString(36).substring(2, 9)}`;
    });
    
    // Function to handle media file selection
    function handleMediaAdd(event) {
        const files = event.target.files;
        if (files && files.length > 0) {
            // Add the new files to our media collection
            const newFiles = Array.from(files).map(file => {
                const fileType = file.type.split('/')[0]; // 'image', 'video', 'audio', etc.
                return {
                    file: file,
                    type: fileType,
                    url: URL.createObjectURL(file),
                    name: file.name
                };
            });
            
            mediaFiles = [...mediaFiles, ...newFiles];
            dispatch('update', { mediaFiles });
        }
        // Reset the input so the same file can be selected again if needed
        event.target.value = '';
    }
    
    // Function to remove a file from the mediaFiles array
    function removeMediaFile(index) {
        URL.revokeObjectURL(mediaFiles[index].url); // Clean up the created URL
        mediaFiles = mediaFiles.filter((_, i) => i !== index);
        dispatch('update', { mediaFiles });
    }
    
    // Clean up object URLs when component is destroyed
    onDestroy(() => {
        // Clean up any created object URLs
        for (const media of mediaFiles) {
            if (media.url) {
                URL.revokeObjectURL(media.url);
            }
        }
    });

    // Helper function for consistent removal icon
    function RemovalIcon() {
        return `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;
    }
</script>

<div class="mb-6">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-2">
        <label for={uniqueId} class="block text-sm font-medium">Upload Media Files*</label>
        <span class="text-xs text-gray-500 mt-1 sm:mt-0">{mediaFiles.length} files selected</span>
    </div>
    
    <div class="border-2 border-dashed rounded-lg p-4 text-center mb-3 bg-gray-50 dark:bg-neutral-900 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-neutral-800 transition-colors">
        <label for={uniqueId} class="cursor-pointer block">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto text-gray-400 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <!-- Different text based on screen size -->
            <span class="text-sm block sm:hidden">Tap to select files</span>
            <span class="text-sm hidden sm:block lg:hidden">Click to select files</span>
            <span class="text-sm hidden lg:block">Click to select files or drag and drop</span>
            <p class="text-xs text-gray-500 mt-1">Images, videos, or audio files</p>
            <input 
                type="file" 
                id={uniqueId} 
                accept="image/*,video/*,audio/*" 
                on:change={handleMediaAdd}
                class="hidden"
                multiple
            />
        </label>
    </div>
    
    {#if errors.media}
        <p class="text-red-500 text-sm mb-3">{errors.media}</p>
    {/if}
    
    <!-- Media Files Preview -->
    {#if mediaFiles.length > 0}
        <div class="mt-4 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
            {#each mediaFiles as mediaItem, index}
                <div class="relative border rounded overflow-hidden bg-white dark:bg-neutral-800 shadow-sm hover:shadow transition-all">
                    <!-- Add consistent removal button to the top-right corner -->
                    <button 
                        type="button"
                        class="absolute top-2 right-2 z-10 h-6 w-6 rounded-full flex items-center justify-center bg-black bg-opacity-50 text-white hover:bg-red-500 transition-colors"
                        on:click={() => removeMediaFile(index)}
                        title="Remove media"
                    >
                        {@html RemovalIcon()}
                    </button>

                    <!-- Media preview with consistent height -->
                    <div class="aspect-square overflow-hidden bg-gray-100 dark:bg-neutral-900 flex items-center justify-center">
                        {#if mediaItem.type === 'image'}
                            <img src={mediaItem.url} alt="Preview" class="w-full h-full object-cover" />
                        {:else if mediaItem.type === 'video'}
                            <!-- svelte-ignore a11y-media-has-caption -->
                            <video src={mediaItem.url} class="w-full h-full object-cover" controls></video>
                        {:else if mediaItem.type === 'audio'}
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                            </svg>
                        {:else}
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        {/if}
                    </div>
                    
                    <!-- File name and controls -->
                    <div class="p-2 border-t">
                        <div class="text-xs truncate font-medium">{mediaItem.name}</div>
                        <div class="mt-1">
                            <span class="text-xs text-gray-500 capitalize">{mediaItem.type}</span>
                        </div>
                    </div>
                </div>
            {/each}
        </div>
    {/if}
</div>
