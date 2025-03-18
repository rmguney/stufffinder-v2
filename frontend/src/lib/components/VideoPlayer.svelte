<script>
    export let src = '';
    export let title = '';
    export let poster = ''; // Optional thumbnail image
    
    // Handle video loading errors
    function handleError(event) {
      console.error('Video loading error:', event);
      event.target.src = ''; // Clear source to prevent repeated error attempts
    }
  </script>
  
  <div class="w-full h-full flex items-center justify-center bg-neutral-900 relative">
    {#if src}
      <video 
        class="max-w-full max-h-full object-contain"
        controls
        on:error={handleError}
        {poster}
      >
        <source src={src} type="video/mp4" />
        <source src={src} type="video/webm" />
        <source src={src} type="video/ogg" />
        Your browser does not support the video tag.
      </video>
    {:else}
      <div class="flex flex-col items-center justify-center p-8 text-white">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-neutral-500 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z" />
        </svg>
        <p class="text-neutral-400 text-center">{title || 'Video unavailable'}</p>
      </div>
    {/if}
  </div>