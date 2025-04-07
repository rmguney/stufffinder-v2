<script>
  import { Textarea } from "$lib/components/ui/textarea";
  import { Button } from "$lib/components/ui/button";
  import * as Card from "$lib/components/ui/card";
  import { createEventDispatcher } from "svelte";
  let dispatch = createEventDispatcher();

export let resolutionDescription;
export let selectedResolutionFiles;
export let resolutionFileInputRef;
export let isResolutionFileLoading;
export let resolutionError;


  // Function to handle file selection for Resolution
  function handleResolutionFileSelect(event) {
    const files = Array.from(event.target.files);
    if (files.length > 0) {
      selectedResolutionFiles = files;
    }
  }

  // resolution submission
  let handleResolutionSubmit = async () => {
    if (!resolutionDescription.trim() && selectedResolutionFiles.length === 0) {
      resolutionError = "Please write description or attach media";
      return;
    }

    isResolutionFileLoading = true;
    resolutionError = null;

    const resolution = {
        resolutionDescription,
        resolutionFileInputRef,
        selectedResolutionFiles,
        id: Math.random()
    };
    dispatch('handleResolutionSubmit', resolution);
      
    // Clear the input and files
    resolutionDescription = '';
    selectedResolutionFiles = [];
    if (resolutionFileInputRef) resolutionFileInputRef.value = '';
}

</script>

<Card.Root class="bg-opacity-90 hover:bg-opacity-100 p-4 mt-4 flex flex-col justify-center items-center">
    <div class="w-full space-y-4">
        <Textarea 
        bind:value={resolutionDescription} 
        class="w-full resize-none p-2" 
        id="resolution-description"
        placeholder="Write description explaining the resolution..."
        />
        
        <!-- Simplified file input for resolution media -->
        <div class="w-full">
        <div class="flex items-center justify-center gap-2">
            <div class="flex items-center space-x-2">
            <input 
                type="file" 
                id="resolution-media-upload"
                bind:this={resolutionFileInputRef}
                on:change={handleResolutionFileSelect}
                accept="image/*,video/*,audio/*" 
                class="hidden" 
                multiple
            />
            <Button 
                variant="outline" 
                size="sm" 
                class="text-xs hover:bg-neutral-100 dark:hover:bg-neutral-800"
                on:click={() => resolutionFileInputRef?.click()}
            >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                </svg>
                Add Media
            </Button>
            {#if selectedResolutionFiles.length > 0}
                <span class="text-xs text-neutral-500">
                {selectedResolutionFiles.length} file{selectedResolutionFiles.length !== 1 ? 's' : ''} selected
                </span>
            {/if}
            </div>
            
            <!-- resolution button -->
            <Button 
            on:click={handleResolutionSubmit} 
            variant="outline"
            size="sm"
            class="text-xs hover:bg-neutral-100 dark:hover:bg-neutral-800"
            disabled={isResolutionFileLoading}
            >
            {#if isResolutionFileLoading}
                <span class="inline-block h-4 w-4 border-2 border-current/30 border-t-current rounded-full animate-spin mr-2"></span>
                {selectedResolutionFiles.length > 0 ? 'Uploading...' : 'Sending...'}
            {:else}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 00-1.414 0L8 12.586 4.707 9.293a1 1 0 10-1.414 1.414l4 4a1 1 0 001.414 0l8-8a1 1 0 000-1.414z" clip-rule="evenodd"/>
            </svg>
                Resolve Object
            {/if}
            </Button>
        </div>
        
        <!-- Simple file preview for resolution -->
        {#if selectedResolutionFiles.length > 0}
            <div class="mt-2 flex flex-wrap gap-2">
            {#each selectedResolutionFiles as file, i}
                <div class="relative bg-neutral-100 dark:bg-neutral-800 rounded p-1 w-16 h-16 flex items-center justify-center">
                {#if file.type.startsWith('image/')}
                    <img 
                    src={URL.createObjectURL(file)} 
                    alt={file.name} 
                    class="max-w-full max-h-full object-contain"
                    />
                {:else if file.type.startsWith('video/')}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                {:else if file.type.startsWith('audio/')}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                    </svg>
                {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                {/if}
                <!-- Remove button -->
                <button 
                    class="absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-4 h-4 flex items-center justify-center text-xs"
                    on:click={() => {
                    selectedResolutionFiles = selectedResolutionFiles.filter((_, index) => index !== i);
                    }}
                >
                    ×
                </button>
                </div>
            {/each}
            </div>
        {/if}
        </div>
        
        {#if resolutionError}
        <p class="text-red-500 text-sm">{resolutionError}</p>
        {/if}
    </div>
</Card.Root>

<style>

</style>