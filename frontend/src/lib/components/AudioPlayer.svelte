<script>
    export let src = '';
    export let title = '';
    
    // Handle audio loading errors
    function handleError(event) {
      console.error('Audio loading error:', event);
      event.target.src = ''; // Clear source to prevent repeated error attempts
    }
  </script>
  
  <div class="w-full h-full flex flex-col items-center justify-center p-6 bg-neutral-100 dark:bg-neutral-800">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-24 w-24 text-rose-500 dark:text-rose-400 mb-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
    </svg>
    
    {#if src}
      <audio 
        controls 
        class="w-full max-w-md mb-4"
        on:error={handleError}
      >
        <source src={src} type="audio/mpeg">
        <source src={src} type="audio/ogg">
        <source src={src} type="audio/wav">
        Your browser does not support the audio element.
      </audio>
    {:else}
      <div class="p-4 text-neutral-500 dark:text-neutral-400 mb-4">
        Audio playback unavailable
      </div>
    {/if}
    
    <p class="text-neutral-700 dark:text-neutral-300 text-center">
      {title || 'Audio file'}
    </p>
  </div>