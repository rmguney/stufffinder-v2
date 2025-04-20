<script>
    import { createEventDispatcher, onMount, onDestroy } from 'svelte';
    import { Button } from "$lib/components/ui/button";
    
    const dispatch = createEventDispatcher();
    
    export let mediaFiles = []; // Holds files and their preview URLs
    export let errors = {};
    
    let dragActive = false;
    let fileInputRef;
    let preventedDefaults = new WeakSet();
    
    function handleDrag(e) {
        if (!preventedDefaults.has(e)) {
            e.preventDefault();
            e.stopPropagation();
            preventedDefaults.add(e);
        }
    }
    
    function handleDragEnter(e) {
        handleDrag(e);
        dragActive = true;
    }
    
    function handleDragLeave(e) {
        handleDrag(e);
        dragActive = false;
    }
    
    function handleDrop(e) {
        handleDrag(e);
        dragActive = false;
        
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            handleFiles(e.dataTransfer.files);
        }
    }
    
    function handleChange(e) {
        if (e.target.files && e.target.files.length > 0) {
            handleFiles(e.target.files);
        }
    }
    
    function handleFiles(files) {
        errors.media = '';
        
        // Convert FileList to Array
        const newFiles = Array.from(files);
        
        // Process each file
        const filesToAdd = newFiles.map(file => {
            // Determine file type
            const fileType = getFileType(file);
            
            return {
                file: file,
                fileName: file.name,
                fileSize: file.size,
                type: fileType,
                url: URL.createObjectURL(file)
            };
        });
        
        // Add new files to the array
        mediaFiles = [...mediaFiles, ...filesToAdd];
        
        // Notify parent component
        dispatch('update', { mediaFiles });
    }
    
    function getFileType(file) {
        if (file.type.startsWith('image/')) {
            return 'image';
        } else if (file.type.startsWith('video/')) {
            return 'video';
        } else if (file.type.startsWith('audio/')) {
            return 'audio';
        } else {
            return 'document';
        }
    }
    
    function removeFile(index) {
        // Create a copy of the array without the removed file
        const updatedFiles = mediaFiles.filter((_, i) => i !== index);
        
        // Revoke the object URL to prevent memory leaks
        URL.revokeObjectURL(mediaFiles[index].url);
        
        // Update state
        mediaFiles = updatedFiles;
        
        // Notify parent component
        dispatch('update', { mediaFiles });
    }
    
    onDestroy(() => {
        // Clean up object URLs when component is unmounted
        mediaFiles.forEach(media => {
            if (media.url) {
                URL.revokeObjectURL(media.url);
            }
        });
    });
</script>

<div class="flex flex-col w-full">
    <!-- File drag & drop area -->
    <div 
        class="border-2 border-dashed rounded-md p-6 text-center transition-all
            {dragActive ? 'border-teal-500 bg-teal-50 dark:bg-teal-900/30' : 'border-neutral-300 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-neutral-900/30'}"
        on:dragenter={handleDragEnter}
        on:dragleave={handleDragLeave}
        on:dragover={handleDrag}
        on:drop={handleDrop}
    >
        <input 
            bind:this={fileInputRef}
            type="file" 
            id="mediaUpload" 
            multiple 
            accept="image/*,video/*,audio/*"
            on:change={handleChange}
            class="hidden"
        />
        
        <label for="mediaUpload" class="cursor-pointer flex flex-col items-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-neutral-400 dark:text-neutral-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            
            <!-- Desktop primary text -->
            <div class="hidden lg:block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                {dragActive ? 'Drop files to upload' : 'Drag and drop files here'}
            </div>
            
            <!-- Mobile primary text -->
            <div class="block lg:hidden text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                Tap to upload media
            </div>
            
            <!-- Desktop secondary text -->
            <div class="hidden lg:block text-xs text-neutral-500 dark:text-neutral-400">
                or <span class="text-teal-600 dark:text-teal-400 hover:underline font-medium">browse files</span>
            </div>
            
            <div class="text-xs text-neutral-500 dark:text-neutral-400 mt-2">
                Supports images, videos, and audio files
            </div>
        </label>
    </div>
    
    <!-- Preview of uploaded files -->
    {#if mediaFiles.length > 0}
        <div class="mt-4">
            <h4 class="text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300">Media Files ({mediaFiles.length})</h4>
            <div class="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 gap-4">
                {#each mediaFiles as media, index}
                    <div class="relative group flex flex-col border border-neutral-200 dark:border-neutral-700 rounded-md overflow-hidden bg-white dark:bg-neutral-900">
                        <!-- Media preview -->
                        <div class="relative h-24 flex items-center justify-center bg-neutral-50 dark:bg-neutral-900 p-2">
                            {#if media.type === 'image'}
                                <img 
                                    src={media.url} 
                                    alt={media.fileName} 
                                    class="max-h-full max-w-full object-contain"
                                />
                            {:else if media.type === 'video'}
                                <div class="flex flex-col items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z" />
                                    </svg>
                                    <span class="text-xs text-neutral-500 mt-1">Video</span>
                                </div>
                            {:else if media.type === 'audio'}
                                <div class="flex flex-col items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                                    </svg>
                                    <span class="text-xs text-neutral-500 mt-1">Audio</span>
                                </div>
                            {:else}
                                <div class="flex flex-col items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                    </svg>
                                    <span class="text-xs text-neutral-500 mt-1">File</span>
                                </div>
                            {/if}
                            
                            <!-- Remove button -->
                            <button 
                                type="button"
                                class="absolute top-1 right-1 bg-white dark:bg-neutral-800 rounded-full h-5 w-5 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 hover:text-red-500 dark:hover:text-red-400"
                                on:click={() => removeFile(index)}
                                title="Remove file"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                                </svg>
                            </button>
                        </div>
                        
                        <!-- File info -->
                        <div class="p-2">
                            <div class="truncate text-xs text-neutral-600 dark:text-neutral-400" title={media.fileName}>{media.fileName}</div>
                            <div class="text-xs text-neutral-500 dark:text-neutral-500">
                                {Math.round(media.fileSize / 1024)} KB
                            </div>
                        </div>
                    </div>
                {/each}
            </div>
        </div>
    {/if}
</div>
