<script>
  export let size = "medium"; // small, medium, large
  
  const sizeMap = {
    small: "w-4 h-4",
    medium: "w-8 h-8",
    large: "w-12 h-12"
  };
  
  const sizeClass = sizeMap[size] || sizeMap.medium;
</script>

<div class="flex justify-center items-center">
  <div class={`${sizeClass} border-4 border-neutral-200 dark:border-neutral-800 border-t-teal-600 dark:border-t-teal-500 rounded-full animate-spin`}></div>
</div>
